package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.common.nowMillis
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.OrderActivityMatchDto
import com.rarible.protocol.dto.PrepareOrderTxFormDto
import com.rarible.protocol.order.core.model.Asset
import com.rarible.protocol.order.core.model.Erc20AssetType
import com.rarible.protocol.order.core.model.Erc721AssetType
import com.rarible.protocol.order.core.model.ItemType
import com.rarible.protocol.order.core.model.OpenSeaOrderFeeMethod
import com.rarible.protocol.order.core.model.OpenSeaOrderHowToCall
import com.rarible.protocol.order.core.model.OpenSeaOrderSaleKind
import com.rarible.protocol.order.core.model.OpenSeaOrderSide
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderOpenSeaV1DataV1
import com.rarible.protocol.order.core.model.OrderSide
import com.rarible.protocol.order.core.model.OrderSideMatch
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.OrderVersion
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.model.Transfer
import com.rarible.protocol.order.core.service.CallDataEncoder
import com.rarible.protocol.order.core.service.CommonSigner
import com.rarible.protocol.order.core.service.PrepareTxService
import com.rarible.protocol.order.listener.integration.IntegrationTest
import com.rarible.protocol.order.listener.misc.sign
import io.daonomic.rpc.domain.Binary
import io.mockk.verify
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import scalether.domain.Address
import scalether.domain.request.Transaction
import java.math.BigInteger

@IntegrationTest
internal class WyvernExchangeOrderMatchDescriptorTest : AbstractOpenSeaV1Test() {
    @Autowired
    private lateinit var prepareTxService: PrepareTxService
    @Autowired
    private lateinit var callDataEncoder: CallDataEncoder
    @Autowired
    private lateinit var commonSigner: CommonSigner

    @Test
    fun `should execute sell order with call method`() = runBlocking {
        val sellMaker = userSender1.from()
        val buyMaker = userSender2.from()
        val target = token721.address()
        val tokenId = EthUInt256.ONE
        val paymentToken = token1.address()

        token721.mint(sellMaker, BigInteger.ONE, "test").execute().verifySuccess()
        token1.mint(buyMaker, BigInteger.TEN.pow(10)).execute().verifySuccess()

        val sellTransfer = Transfer.Erc721Transfer(
            from = sellMaker,
            to = Address.ZERO(),
            tokenId = tokenId.value,
            safe = false
        )
        val sellCallData = callDataEncoder.encodeTransferCallData(sellTransfer)

        val sellOrderVersion = OrderVersion(
            maker = sellMaker,
            taker = null,
            make = Asset(Erc721AssetType(target, tokenId), EthUInt256.ONE),
            take = Asset(Erc20AssetType(paymentToken), EthUInt256.TEN),
            type = OrderType.OPEN_SEA_V1,
            salt = EthUInt256.TEN,
            start = nowMillis().epochSecond - 10,
            end = null,
            signature = null,
            data = OrderOpenSeaV1DataV1(
                exchange = exchange.address(),
                makerRelayerFee = BigInteger.valueOf(250),
                takerRelayerFee = BigInteger.ZERO,
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = exchange.address(),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.SELL,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.CALL,
                callData = sellCallData.callData,
                replacementPattern = sellCallData.replacementPattern,
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO,
                target = null,
                nonce = null,
            ),
            platform = Platform.OPEN_SEA,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        ).let {
            val hash = Order.hash(it) // Recalculate OpenSea's specific order hash
            val hashToSign = commonSigner.ethSignHashToSign(hash)
            logger.info("Sell order hash: $hash, hash to sing: $hashToSign")
            val signature = hashToSign.sign(privateKey1)
            it.copy(signature = signature, hash = hash)
        }

        val sellOrder = orderUpdateService.save(sellOrderVersion)

        val form = PrepareOrderTxFormDto(
            maker = buyMaker,
            amount = BigInteger.TEN,
            originFees = emptyList(),
            payouts = emptyList()
        )
        val response = prepareTxService.prepareTransaction(sellOrder, form)

        userSender2.sendTransaction(
            Transaction(
                exchange.address(),
                userSender2.from(),
                8000000.toBigInteger(),
                BigInteger.ONE,
                BigInteger.ZERO,
                response.transaction.data,
                null
            )
        ).verifySuccess()

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(items).hasSize(2)

            val map = items
                .map { it.data as OrderSideMatch }
                .associateBy { it.side }

            val left = map[OrderSide.LEFT]
            val right = map[OrderSide.RIGHT]

            assertThat(left?.fill).isEqualTo(EthUInt256.TEN)
            assertThat(right?.fill).isEqualTo(EthUInt256.ONE)

            assertThat(left?.make)
                .isEqualTo(sellOrder.make)
            assertThat(left?.take)
                .isEqualTo(sellOrder.take)
            assertThat(left?.externalOrderExecutedOnRarible).isTrue()

            assertThat(right?.make)
                .isEqualTo(sellOrder.take)
            assertThat(right?.take)
                .isEqualTo(sellOrder.make)
            assertThat(right?.externalOrderExecutedOnRarible).isTrue()

            val filledOrder = orderRepository.findById(sellOrder.hash)
            assertThat(filledOrder?.fill).isEqualTo(EthUInt256.TEN)

            assertFalse(left?.adhoc!!)
            assertTrue(left.counterAdhoc!!)

            assertTrue(right?.adhoc!!)
            assertFalse(right.counterAdhoc!!)

            checkActivityWasPublished {
                assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                    assertThat(left.hash).isEqualTo(sellOrder.hash)
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `should execute sell order with delegate call method`(safeTransfer: Boolean) = runBlocking {
        val sellMaker = userSender1.from()
        val buyMaker = userSender2.from()
        val target = token721.address()
        val tokenId = EthUInt256.ONE
        val paymentToken = token1.address()

        token721.mint(sellMaker, BigInteger.ONE, "test").execute().verifySuccess()
        token1.mint(buyMaker, BigInteger.TEN.pow(10)).execute().verifySuccess()

        val sellTransfer = Transfer.MerkleValidatorErc721Transfer(
            from = sellMaker,
            to = Address.ZERO(),
            token = token721.address(),
            tokenId = tokenId.value,
            root = WORD_ZERO,
            proof = emptyList(),
            safe = safeTransfer
        )
        val sellCallData = callDataEncoder.encodeTransferCallData(sellTransfer)

        val sellOrderVersion = OrderVersion(
            maker = sellMaker,
            taker = null,
            make = Asset(Erc721AssetType(target, tokenId), EthUInt256.ONE),
            take = Asset(Erc20AssetType(paymentToken), EthUInt256.TEN),
            type = OrderType.OPEN_SEA_V1,
            salt = EthUInt256.TEN,
            start = nowMillis().epochSecond - 10,
            end = null,
            signature = null,
            data = OrderOpenSeaV1DataV1(
                exchange = exchange.address(),
                makerRelayerFee = BigInteger.valueOf(250),
                takerRelayerFee = BigInteger.ZERO,
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = exchange.address(),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.SELL,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.DELEGATE_CALL,
                callData = sellCallData.callData,
                replacementPattern = sellCallData.replacementPattern,
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO,
                target = merkleValidator.address(),
                nonce = null,
            ),
            platform = Platform.OPEN_SEA,
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        ).let {
            val hash = Order.hash(it) // Recalculate OpenSea's specific order hash
            val hashToSign = commonSigner.ethSignHashToSign(hash)
            logger.info("Sell order hash: $hash, hash to sing: $hashToSign")
            val signature = hashToSign.sign(privateKey1)
            it.copy(signature = signature, hash = hash)
        }

        val sellOrder = orderUpdateService.save(sellOrderVersion)

        val form = PrepareOrderTxFormDto(
            maker = buyMaker,
            amount = BigInteger.TEN,
            originFees = emptyList(),
            payouts = emptyList()
        )
        val response = prepareTxService.prepareTransaction(sellOrder, form)

        userSender2.sendTransaction(
            Transaction(
                exchange.address(),
                userSender2.from(),
                8000000.toBigInteger(),
                BigInteger.ONE,
                BigInteger.ZERO,
                response.transaction.data,
                null
            )
        ).verifySuccess()

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(items).hasSize(2)

            val map = items
                .map { it.data as OrderSideMatch }
                .associateBy { it.side }

            val left = map[OrderSide.LEFT]
            val right = map[OrderSide.RIGHT]

            assertThat(left?.fill).isEqualTo(EthUInt256.TEN)
            assertThat(right?.fill).isEqualTo(EthUInt256.ONE)

            assertThat(left?.make)
                .isEqualTo(sellOrder.make)
            assertThat(left?.take)
                .isEqualTo(sellOrder.take)
            assertThat(left?.externalOrderExecutedOnRarible).isTrue()

            assertThat(right?.make)
                .isEqualTo(sellOrder.take)
            assertThat(right?.take)
                .isEqualTo(sellOrder.make)
            assertThat(right?.externalOrderExecutedOnRarible).isTrue()

            val filledOrder = orderRepository.findById(sellOrder.hash)
            assertThat(filledOrder?.fill).isEqualTo(EthUInt256.TEN)

            assertFalse(left?.adhoc!!)
            assertTrue(left.counterAdhoc!!)

            assertTrue(right?.adhoc!!)
            assertFalse(right.counterAdhoc!!)

            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())
            }

            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())
            }

            checkActivityWasPublished {
                assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                    assertThat(left.hash).isEqualTo(sellOrder.hash)
                }
            }
        }
    }

    @Test
    fun `should execute buy order`() = runBlocking {
        val sellMaker = userSender1.from()
        val buyMaker = userSender2.from()
        val target = token721.address()
        val tokenId = EthUInt256.ONE
        val paymentToken = token1.address()

        token721.mint(sellMaker, BigInteger.ONE, "test").execute().verifySuccess()
        token1.mint(buyMaker, BigInteger.TEN.pow(10)).execute().verifySuccess()
        token1.mint(sellMaker, BigInteger.TEN.pow(10)).execute().verifySuccess()

        val buyTransfer = Transfer.Erc721Transfer(
            from = Address.ZERO(),
            to = buyMaker,
            tokenId = tokenId.value,
            safe = false
        )
        val buyCallData = callDataEncoder.encodeTransferCallData(buyTransfer)

        val buyOrderVersion = OrderVersion(
            maker = buyMaker,
            taker = null,
            make = Asset(Erc20AssetType(paymentToken), EthUInt256.TEN),
            take = Asset(Erc721AssetType(target, tokenId), EthUInt256.ONE),
            type = OrderType.OPEN_SEA_V1,
            salt = EthUInt256.TEN,
            start = nowMillis().epochSecond - 10,
            end = null,
            signature = null,
            data = OrderOpenSeaV1DataV1(
                exchange = exchange.address(),
                makerRelayerFee = BigInteger.ZERO,
                takerRelayerFee = BigInteger.valueOf(250),
                makerProtocolFee = BigInteger.ZERO,
                takerProtocolFee = BigInteger.ZERO,
                feeRecipient = exchange.address(),
                feeMethod = OpenSeaOrderFeeMethod.SPLIT_FEE,
                side = OpenSeaOrderSide.BUY,
                saleKind = OpenSeaOrderSaleKind.FIXED_PRICE,
                howToCall = OpenSeaOrderHowToCall.CALL,
                callData = buyCallData.callData,
                replacementPattern = buyCallData.replacementPattern,
                staticTarget = Address.ZERO(),
                staticExtraData = Binary.apply(),
                extra = BigInteger.ZERO,
                target = null,
                nonce = null,
            ),
            createdAt = nowMillis(),
            makePriceUsd = null,
            takePriceUsd = null,
            makePrice = null,
            takePrice = null,
            makeUsd = null,
            takeUsd = null
        ).let {
            val hash = Order.hash(it) // Recalculate OpenSea's specific order hash
            val hashToSign = commonSigner.ethSignHashToSign(hash)
            logger.info("Buy order hash: $hash, hash to sing: $hashToSign")
            val signature = hashToSign.sign(privateKey2)
            it.copy(signature = signature, hash = hash)
        }

        val buyOrder = orderUpdateService.save(buyOrderVersion)

        val form = PrepareOrderTxFormDto(
            maker = sellMaker,
            amount = BigInteger.ONE,
            originFees = emptyList(),
            payouts = emptyList()
        )
        val response = prepareTxService.prepareTransaction(buyOrder, form)

        userSender1.sendTransaction(
            Transaction(
                exchange.address(),
                userSender1.from(),
                8000000.toBigInteger(),
                BigInteger.ONE,
                BigInteger.ZERO,
                response.transaction.data,
                null
            )
        ).verifySuccess()

        Wait.waitAssert {
            val items = exchangeHistoryRepository.findByItemType(ItemType.ORDER_SIDE_MATCH).collectList().awaitFirst()
            assertThat(items).hasSize(2)

            val map = items
                .map { it.data as OrderSideMatch }
                .associateBy { it.side }

            val left = map[OrderSide.LEFT]
            val right = map[OrderSide.RIGHT]

            assertThat(left?.fill).isEqualTo(EthUInt256.ONE)
            assertThat(right?.fill).isEqualTo(EthUInt256.TEN)

            assertThat(left?.make)
                .isEqualTo(buyOrder.make)
            assertThat(left?.take)
                .isEqualTo(buyOrder.take)
            assertThat(left?.externalOrderExecutedOnRarible).isTrue()
            assertThat(left?.origin).isEqualTo(Platform.RARIBLE.id)

            assertThat(right?.make)
                .isEqualTo(buyOrder.take)
            assertThat(right?.take)
                .isEqualTo(buyOrder.make)
            assertThat(right?.externalOrderExecutedOnRarible).isTrue()
            assertThat(left?.origin).isEqualTo(Platform.RARIBLE.id)

            val filledOrder = orderRepository.findById(buyOrder.hash)
            assertThat(filledOrder?.fill).isEqualTo(EthUInt256.ONE)

            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                left?.let{currencyApi.getCurrencyRate(any(), any(), left.date.toEpochMilli())}
            }
            verify {
                @Suppress("ReactiveStreamsUnusedPublisher")
                right?.let{currencyApi.getCurrencyRate(any(), any(), right.date.toEpochMilli())}
            }

            checkActivityWasPublished {
                assertThat(this).isInstanceOfSatisfying(OrderActivityMatchDto::class.java) {
                    assertThat(it.left.hash).isEqualTo(buyOrder.hash)
                }
            }
        }
    }
}
