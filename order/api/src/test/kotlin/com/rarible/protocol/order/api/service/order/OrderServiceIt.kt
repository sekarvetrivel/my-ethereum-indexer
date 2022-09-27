package com.rarible.protocol.order.api.service.order

import com.rarible.contracts.test.erc1271.TestERC1271
import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomWord
import com.rarible.core.test.wait.Wait
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.nft.domain.EIP712DomainNftFactory
import com.rarible.protocol.dto.Continuation
import com.rarible.protocol.dto.Erc20DecimalBalanceDto
import com.rarible.protocol.dto.LazyErc721Dto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.dto.PartDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.order.api.data.sign
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.core.converters.dto.PlatformDtoConverter
import com.rarible.protocol.order.core.converters.model.LazyAssetTypeToLazyNftConverter
import com.rarible.protocol.order.core.data.createNftCollectionDto
import com.rarible.protocol.order.core.data.createNftItemDto
import com.rarible.protocol.order.core.data.createNftOwnershipDto
import com.rarible.protocol.order.core.misc.ownershipId
import com.rarible.protocol.order.core.misc.platform
import com.rarible.protocol.order.core.model.*
import com.rarible.protocol.order.core.model.order.OrderFilterAll
import com.rarible.protocol.order.core.model.order.OrderFilterSell
import com.rarible.protocol.order.core.model.order.OrderFilterSellByCollection
import com.rarible.protocol.order.core.model.order.OrderFilterSellByItem
import com.rarible.protocol.order.core.model.order.OrderFilterSellByMaker
import com.rarible.protocol.order.core.model.order.OrderFilterSort
import com.rarible.protocol.order.core.producer.ProtocolOrderPublisher
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.allAndAwait
import org.springframework.data.mongodb.core.remove
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.domain.AddressFactory
import java.math.BigInteger
import java.util.stream.Stream
import kotlin.random.Random
import kotlin.streams.asStream

@IntegrationTest
class OrderServiceIt : AbstractOrderIt() {

    @Autowired
    private lateinit var orderService: OrderService

    @Autowired
    private lateinit var protocolOrderPublisher: ProtocolOrderPublisher

    @BeforeEach
    fun beforeEach() = runBlocking {
        runBlocking<Unit> {
            orderVersionRepository.deleteAll().awaitFirstOrNull()
            mongo.remove<Order>().allAndAwait()
        }
        orderRepository.createIndexes()
    }

    @BeforeEach
    fun mockBalances() {
        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns MakeBalanceState(EthUInt256.TEN)
    }

    companion object {
        @JvmStatic
        fun allPlatform(): Stream<Platform> {
            return Platform.values().asSequence().asStream()
        }
    }

    @Test
    fun incorrectSignature() = runBlocking<Unit> {
        val (privateKey) = generateNewKeys()
        val order = createOrder(AddressFactory.create())

//        assertThrows<OrderUpdateException> {
//            runBlocking {
//                orderService.put(order.toForm(privateKey))
//            }
//        }

        //rarible
//        val make = Asset(Erc721AssetType(token = AddressParser.parse("0x6ede7f3c26975aad32a475e1021d8f6f39c89d82"),
//            tokenId = EthUInt256(BigInteger("111808197578425989514687942573564522107819172963540701747326627703587224944657"))),
//            value = EthUInt256.ONE)
//        val take = Asset(
//            type = EthAssetType,
//            value = EthUInt256(BigInteger("10000000000000"))
//        )//Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5))
//        val signature = Binary.apply("0x075d7a2b53dbb86e0d58541d77afbddb970fdca788ccbab00bbb04e0ca39258734947a8315c0f4438050164b85dd942215257305a056df7f7eb1de5ed7d689671b")//CommonSigner().hashToSign(orderVersion.hash).sign(privateKey)
        val make = Asset(Erc721AssetType(token = AddressParser.parse("0xdb3602bfa17ca9c42b11d3cb13199b79ff044816"),
            tokenId = EthUInt256(BigInteger("5200435271198095841614392404294920383202070526176298022107874038349490552833"))),
            value = EthUInt256.ONE)
        val take = Asset(
            type = EthAssetType,
            value = EthUInt256(BigInteger("3000000000000000"))
        )//Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5))
        val signature = Binary.apply("0xsekara00384d722bc80dc619056e9630e26457a6a3e64b4ecf2c068b3853d3943a6caa59f9376c51e26c2b30d86d31c244c6bc613df87f2d1e8b379b474b25361c")//CommonSigner().hashToSign(orderVersion.hash).sign(privateKey)
        val orderVersion = createOrderVersion(make, take, signature)
        orderSignatureValidator.validate(orderVersion)
    }

    fun createOrderVersion(make: Asset, take: Asset, signature: Binary) = OrderVersion(
        hash = Word.apply("0xcb9f982895299fef2e910ce853c949514e84d3adb1248f57766c3ea891dsekar"),//"0x3f3b5e1d235519411581a24bb076ad00446733d5cb808cfe496d60b5a7c9e628"),
        maker = AddressParser.parse("0x0b7f5790f119aa05b581c9e315437cf500449645"),
        taker = AddressParser.parse("0x0000000000000000000000000000000000000000"),
        makePriceUsd = (1..100).random().toBigDecimal(),
        takePriceUsd = (1..100).random().toBigDecimal(),
        makePrice = (1..100).random().toBigDecimal(),
        takePrice = (1..100).random().toBigDecimal(),
        makeUsd = (1..100).random().toBigDecimal(),
        takeUsd = (1..100).random().toBigDecimal(),
        make = make,
        take = take,
        platform = Platform.RARIBLE,
        type = OrderType.RARIBLE_V2,
        salt = EthUInt256(BigInteger("8536378396962972402759257607167356206636298803736733086932209599055216456058")),
        start = null,
        end = null,
        data = OrderRaribleV2DataV2(
            payouts = emptyList(),
            originFees =  emptyList(),
            isMakeFill = true
        ),
        signature = signature)

    fun createOrder() =
        Order(
            maker = AddressParser.parse("0x0b7f5790f119aa05b581c9e315437cf500449645"), //AddressFactory.create(),
            taker = null,
            make = createMake("1"),
            take = createEth(),
            makeStock = EthUInt256(BigInteger("1")),
            type = OrderType.RARIBLE_V2,
            fill = EthUInt256.ZERO,
            cancelled = false,
            salt = EthUInt256(BigInteger("57575976112221712031749901136025211353311563194191846159454969863334581796983")),
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            lastUpdateAt = nowMillis()
        )

    fun createEth(): Asset {
        return Asset(
            type = EthAssetType,
            value = EthUInt256(BigInteger("1"))
        )
    }
    fun createMake(value:String): Asset {
        return Asset(
            type = createAssetType("0xa7e550df5593049dcf7f8088d0272c7d8f353cd2"),
            value = EthUInt256(BigInteger(value))
        )
    }

    fun createAssetType(address:String): AssetType {
        return Erc721AssetType(token = AddressParser.parse(address),
            tokenId = EthUInt256(BigInteger("5200435271198095841614392404294920383202070526176298022107874038349490552835")))
    }


    @Test
    fun saveOrderAndVersion() = runBlocking<Unit> {
        val (privateKey, _, signer) = generateNewKeys()

        val order = createOrder(signer)
        val saved = orderService.put(order.toForm(privateKey))

        assertThat(saved.hash).isEqualTo(order.hash)

        assertThat(orderVersionRepository.count().awaitFirst()).isEqualTo(1)

        val version = orderVersionRepository.findAll().awaitFirst()
        assertThat(version.make.value)
            .isEqualTo(order.make.value)
        assertThat(version.take.value)
            .isEqualTo(order.take.value)

        coVerify {
            protocolOrderPublisher.publish(withArg<OrderActivityDto> {
                assertThat(it.id).isEqualTo(version.id.toString())
            })
        }

        val changed = order.copy(
            make = order.make.copy(value = EthUInt256.of(20)),
            take = order.take.copy(value = EthUInt256.ONE)
        )
        val updated = orderService.put(changed.toForm(privateKey))
        assertThat(updated.make.value)
            .isEqualTo(EthUInt256.of(20))
        assertThat(updated.take.value)
            .isEqualTo(EthUInt256.ONE)

        assertThat(orderVersionRepository.count().awaitFirst()).isEqualTo(2)

        coVerify(atLeast = 2) { protocolOrderPublisher.publish(any<OrderActivityDto>()) }
    }

    @Test
    fun `create order with price history`() = runBlocking<Unit> {
        val (privateKey, _, signer) = generateNewKeys()

        val order = createOrder(signer)
        orderService.put(order.toForm(privateKey))
        val saved = orderRepository.findById(order.hash)!!

        assertThat(saved.priceHistory.size).isEqualTo(1)
        val firstPriceRecord = saved.priceHistory[0]

        assertThat(firstPriceRecord.makeValue.toBigInteger()).isEqualTo(order.make.value.value)
        assertThat(firstPriceRecord.takeValue.toBigInteger()).isEqualTo(order.take.value.value)
    }

    @Test
    fun `update order with price history - prices not changed`() = runBlocking<Unit> {
        val (privateKey, _, signer) = generateNewKeys()

        val order = createOrder(signer)
        orderService.put(order.toForm(privateKey))
        val saved = orderRepository.findById(order.hash)!!

        assertThat(saved.priceHistory.size).isEqualTo(1)

        orderService.put(saved.toForm(privateKey))
        val updated = orderRepository.findById(order.hash)!!

        // Prices are not changed, no new record should be added
        assertThat(updated.priceHistory.size).isEqualTo(1)
    }

    @Test
    fun `update order with price history - price changed`() = runBlocking<Unit> {
        val (privateKey, _, signer) = generateNewKeys()

        val order = createOrder(signer, Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(100)))
        orderService.put(order.toForm(privateKey))
        val saved = orderRepository.findById(order.hash)!!

        assertThat(saved.priceHistory.size).isEqualTo(1)
        val savedWithNewPrice = saved.copy(make = saved.make.copy(value = EthUInt256.of(200)))

        orderService.put(savedWithNewPrice.toForm(privateKey))
        val updated = orderRepository.findById(order.hash)!!

        // Price changed, new record should be added to the price history
        assertThat(updated.priceHistory.size).isEqualTo(2)
    }

    @Test
    fun `update order with price history - history limit reached`() = runBlocking<Unit> {
        val (privateKey, _, signer) = generateNewKeys()

        val order = createOrder(signer, Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(10)))
        orderService.put(order.toForm(privateKey))

        for (i in 1..25) {
            val saved = orderRepository.findById(order.hash)!!
            val updatedTakeAsset = saved.make.copy(value = EthUInt256.of(100 * i))
            val savedWithNewPrice = saved.copy(make = updatedTakeAsset)
            orderService.put(savedWithNewPrice.toForm(privateKey))
        }

        val updated = orderRepository.findById(order.hash)!!

        // Checking old history records are removed
        assertThat(updated.priceHistory.size).isEqualTo(20)
        // Checking last update is on the top
        assertThat(updated.priceHistory[0].makeValue.toLong()).isEqualTo(25 * 100)
    }

    @Test
    fun `get orders by ids`() = runBlocking<Unit> {
        val (privateKey1, _, signer1) = generateNewKeys()
        val (privateKey2, _, signer2) = generateNewKeys()

        val order1 = createOrder(signer1)
        val order2 = createOrder(signer2)

        orderService.put(order1.toForm(privateKey1))
        orderService.put(order2.toForm(privateKey2))

        val orders = orderClient.getOrdersByIds(
            OrderIdsDto(
                listOf(
                    order2.hash,
                    Word.apply(randomWord()),
                    order1.hash
                )
            )
        ).collectList().awaitFirst()

        // Non-existing Order is omitted
        assertThat(orders).hasSize(2)
        assertThat(orders).anySatisfy { assertThat(it.hash).isEqualTo(order1.hash) }
        assertThat(orders).anySatisfy { assertThat(it.hash).isEqualTo(order2.hash) }
    }

    @Test
    fun `get all orders sorted by db updated asc`() = runBlocking<Unit> {
        val orderQuantity = 5
        repeat(orderQuantity) {
            saveRandomOrder()
            delay(50)
        }

        val filter = OrderFilterAll(
            sort = OrderFilterSort.DB_UPDATE_ASC,
            platforms = listOf(PlatformDto.RARIBLE, PlatformDto.CRYPTO_PUNKS, PlatformDto.OPEN_SEA)
        )

        val result = orderService.findOrders(filter, 5, null)

        assertThat(result).hasSize(orderQuantity)
        assertThat(result).isSortedAccordingTo { o1, o2 -> o1.dbUpdatedAt!!.compareTo(o2.dbUpdatedAt) }
    }

    @Test
    fun `get all orders sorted by db updated desc`() = runBlocking<Unit> {

        val orderQuantity = 5
        repeat(orderQuantity) {
            saveRandomOrder()
            delay(50)
        }

        val filter = OrderFilterAll(
            sort = OrderFilterSort.DB_UPDATE_DESC,
            platforms = listOf(PlatformDto.RARIBLE, PlatformDto.CRYPTO_PUNKS, PlatformDto.OPEN_SEA)
        )

        val result = orderService.findOrders(filter, orderQuantity, null)

        assertThat(result).hasSize(orderQuantity)
        assertThat(result).isSortedAccordingTo { o1, o2 -> o2.dbUpdatedAt!!.compareTo(o1.dbUpdatedAt) }
    }

    @Test
    fun `get all orders - ok`() = runBlocking<Unit> {
        saveRandomOrderWithMakeBalance()
        saveRandomOrderWithMakeBalance()
        saveRandomOrderWithMakeBalance()

        val page1 = orderClient.getOrdersAll(null, null, null, 2).awaitFirst()

        assertThat(page1.continuation).isNotNull()
        assertThat(page1.orders.size).isEqualTo(2)

        val page2 = orderClient.getOrdersAll(null, null, page1.continuation, 2).awaitFirst()

        assertThat(page2.continuation).isNull()
        assertThat(page2.orders.size).isEqualTo(1)

        val sortedOrder1 = page1.orders[0]
        val sortedOrder2 = page1.orders[1]
        val sortedOrder3 = page2.orders[0]

        assertThat(sortedOrder1.lastUpdateAt).isAfterOrEqualTo(sortedOrder2.lastUpdateAt)
        assertThat(sortedOrder2.lastUpdateAt).isAfterOrEqualTo(sortedOrder3.lastUpdateAt)
    }

    @ParameterizedTest
    @MethodSource("allPlatform")
    fun `get orders by platform`(platform: Platform) = runBlocking<Unit> {
        val platformDto = PlatformDtoConverter.convert(platform)

        saveRandomOrderWithMakeBalance()
        saveRandomOrderWithMakeBalance()
        saveRandomOrderWithMakeBalance()

        saveRandomOpenSeaOrderWithMakeBalance()
        saveRandomOpenSeaOrderWithMakeBalance()
        saveRandomOpenSeaOrderWithMakeBalance()

        saveRandomCryptoPunkOrderWithMakeBalance()
        saveRandomCryptoPunkOrderWithMakeBalance()
        saveRandomCryptoPunkOrderWithMakeBalance()

        saveRandomX2Y2OrderWithMakeBalance()
        saveRandomX2Y2OrderWithMakeBalance()
        saveRandomX2Y2OrderWithMakeBalance()

        saveRandomLooksrareOrderWithMakeBalance()
        saveRandomLooksrareOrderWithMakeBalance()
        saveRandomLooksrareOrderWithMakeBalance()

        val page1 = orderClient.getOrdersAll(null, platformDto, null, 2).awaitFirst()

        assertThat(page1.continuation).isNotNull()
        assertThat(page1.orders.size).isEqualTo(2)

        val page2 = orderClient.getOrdersAll(null, platformDto, page1.continuation, 2).awaitFirst()

        assertThat(page2.continuation).isNull()
        assertThat(page2.orders.size).isEqualTo(1)

        val sortedOrder1 = page1.orders[0]
        val sortedOrder2 = page1.orders[1]
        val sortedOrder3 = page2.orders[0]

        assertThat(sortedOrder1.platform).isEqualTo(platformDto)
        assertThat(sortedOrder2.platform).isEqualTo(platformDto)
        assertThat(sortedOrder3.platform).isEqualTo(platformDto)

        assertThat(sortedOrder1.lastUpdateAt).isAfterOrEqualTo(sortedOrder2.lastUpdateAt)
        assertThat(sortedOrder2.lastUpdateAt).isAfterOrEqualTo(sortedOrder3.lastUpdateAt)
    }

    @Test
    fun `get rarible orders by default`() = runBlocking<Unit> {
        saveRandomOrderWithMakeBalance()
        saveRandomOrderWithMakeBalance()
        saveRandomOrderWithMakeBalance()

        saveRandomOpenSeaOrderWithMakeBalance()
        saveRandomOpenSeaOrderWithMakeBalance()
        saveRandomOpenSeaOrderWithMakeBalance()

        val pages = orderClient.getOrdersAll(null, null, null, Int.MAX_VALUE).awaitFirst()

        assertThat(pages.continuation).isNull()
        assertThat(pages.orders.size).isEqualTo(3)

        pages.orders.forEach {
            assertThat(it.platform).isEqualTo(PlatformDto.RARIBLE)
        }
    }

    @Test
    fun `should change lastUpdateAt field`() = runBlocking<Unit> {
        val (privateKey, _, signer) = generateNewKeys()

        val order = createOrder(signer)
        val saved = orderService.put(order.toForm(privateKey))

        val changed = order.copy(
            make = order.make.copy(value = EthUInt256.of(20)),
            take = order.take.copy(value = EthUInt256.ONE)
        )
        val updated = orderService.put(changed.toForm(privateKey))

        assertThat(updated.lastUpdateAt).isAfterOrEqualTo(saved.lastUpdateAt)
    }

    @Test
    fun `should fill takeUsdPrice and makeUsd for bid order`() = runBlocking {
        val (privateKey, _, signer) = generateNewKeys()

        val make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5))
        val take = Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(5))
        val order = createOrder(signer).copy(make = make, take = take)

        val saved = orderService.put(order.toForm(privateKey))
        assertThat(saved.takePriceUsd).isNotNull()
        assertThat(saved.makeUsd).isNotNull()

        assertThat(saved.makePriceUsd).isNull()
        assertThat(saved.takeUsd).isNull()
    }

    @Test
    fun `should fill makeUsdPrice and takeUsd for sell order`() = runBlocking {
        val (privateKey, _, signer) = generateNewKeys()

        val make = Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(5))
        val take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5))
        val order = createOrder(signer).copy(make = make, take = take)

        val saved = orderService.put(order.toForm(privateKey))
        assertThat(saved.makePriceUsd).isNotNull()
        assertThat(saved.takeUsd).isNotNull()

        assertThat(saved.takePriceUsd).isNull()
        assertThat(saved.makeUsd).isNull()
    }

    @Test
    fun saveOrderWithMakerAsContract() = runBlocking<Unit> {
        val signer = createMonoSigningTransactionSender()
        val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))

        val contract = TestERC1271.deployAndWait(signer, createMonoTransactionPoller()).awaitFirst()
        contract
            .setReturnSuccessfulValidSignature(true)
            .execute().verifySuccess()

        val order = createOrder(contract.address())
        val saved = orderService.put(order.toForm(privateKey))

        assertThat(saved.hash).isEqualTo(order.hash)
    }

    @Test
    fun saveAndUpdateLegacyOrder() = runBlocking<Unit> {
        val (privateKey, _, signer) = generateNewKeys()

        val order = createOrder(signer).copy(type = OrderType.RARIBLE_V1, data = OrderDataLegacy(1))
        val saved = orderService.put(order.toForm(privateKey))

        assertThat(saved.hash).isEqualTo(order.hash)

        val changed = order.copy(
            make = order.make.copy(value = EthUInt256.of(20)),
            take = order.take.copy(value = EthUInt256.ONE)
        )
        val updated = orderService.put(changed.toForm(privateKey))
        assertThat(updated.make.value).isEqualTo(EthUInt256.of(20))
        assertThat(updated.take.value).isEqualTo(EthUInt256.ONE)
    }

    @Test
    fun `should set erc20 stock`() = runBlocking<Unit> {
        val (privateKey, _, maker) = generateNewKeys()

        val makerErc20Contract = AddressFactory.create()
        val makerErc20Stock = EthUInt256.of(7)
        val makerBalance = Erc20DecimalBalanceDto(
            makerErc20Contract,
            maker,
            makerErc20Stock.value,
            makerErc20Stock.value.toBigDecimal()
        )

        val order = createOrder(maker)
            .copy(
                maker = maker,
                make = Asset(Erc20AssetType(makerErc20Contract), EthUInt256.TEN),
                take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5))
            )

        coEvery {
            assetMakeBalanceProvider.getMakeBalance(any())
        } returns MakeBalanceState(makerErc20Stock)

        val saved = orderService.put(order.toForm(privateKey))

        assertThat(saved.makeStock).isEqualTo(EthUInt256.of(6))
    }

    @Test
    fun `should set erc721 stock`() = runBlocking<Unit> {
        val (privateKey, _, maker) = generateNewKeys()

        val makerErc721Contract = AddressFactory.create()
        val makerErc721TokenId = EthUInt256.TEN
        val makerErc721Supply = EthUInt256.of(1)
        val erc721AssetType = Erc721AssetType(makerErc721Contract, makerErc721TokenId)
        val nft = createNftOwnershipDto().copy(value = makerErc721Supply.value)

        val order = createOrder(maker)
            .copy(
                maker = maker,
                make = Asset(erc721AssetType, EthUInt256.TEN),
                take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(10))
            )

        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns MakeBalanceState(makerErc721Supply)

        val saved = orderService.put(order.toForm(privateKey))

        assertThat(saved.makeStock).isEqualTo(makerErc721Supply)
    }

    // Due to RPN-1402 we do not reset makeStock
    @Disabled
    @Test
    fun `makeStock should by 0`() = runBlocking<Unit> {
        val (privateKey, _, maker) = generateNewKeys()

        val makerErc721Contract = AddressFactory.create()
        val makerErc721TokenId = EthUInt256.TEN
        val makerErc721Supply = EthUInt256.of(1)
        val erc721AssetType = Erc721AssetType(makerErc721Contract, makerErc721TokenId)
        val nft = createNftOwnershipDto().copy(value = makerErc721Supply.value)

        // order doesn't belong the current start,end interval
        val order = createOrder(maker, Long.MAX_VALUE-1, Long.MAX_VALUE)
            .copy(
                maker = maker,
                make = Asset(erc721AssetType, EthUInt256.TEN),
                take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(10))
            )



        every { nftOwnershipApi.getNftOwnershipById(eq(erc721AssetType.ownershipId(maker)), any()) } returns Mono.just(nft)

        val saved = orderService.put(order.toForm(privateKey))

        assertThat(saved.makeStock).isEqualTo(EthUInt256.ZERO)
    }

    @Test
    fun `should set erc1155 stock`() = runBlocking<Unit> {
        val (privateKey, _, maker) = generateNewKeys()

        val makerErc1155Contract = AddressFactory.create()
        val makerErc1155TokenId = EthUInt256.TEN
        val makerErc1155Supply = EthUInt256.of(7)
        val erc1155AssetType = Erc1155AssetType(makerErc1155Contract, makerErc1155TokenId)
        val nft = createNftOwnershipDto().copy(value = makerErc1155Supply.value)

        val order = createOrder(maker)
            .copy(
                maker = maker,
                make = Asset(erc1155AssetType, EthUInt256.ONE),
                take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5))
            )

        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returns MakeBalanceState(makerErc1155Supply)

        val saved = orderService.put(order.toForm(privateKey))

        assertThat(saved.makeStock).isEqualTo(EthUInt256.ONE)
    }

    @Test
    fun `should update sale order make stock`() = runBlocking<Unit> {
        val (privateKey, _, maker) = generateNewKeys()

        val makerErc1155Contract = AddressFactory.create()
        val makerErc1155TokenId = EthUInt256.TEN
        val erc1155AssetType = Erc1155AssetType(makerErc1155Contract, makerErc1155TokenId)

        val order = createOrder(maker)
            .copy(
                maker = maker,
                make = Asset(erc1155AssetType, EthUInt256.of(100)),
                take = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(100))
            )

        coEvery { assetMakeBalanceProvider.getMakeBalance(any()) } returnsMany listOf(
            MakeBalanceState(EthUInt256.ONE), MakeBalanceState(EthUInt256.TEN)
        )

        val saved = orderService.put(order.toForm(privateKey))
        assertThat(saved.makeStock).isEqualTo(EthUInt256.ONE)

        val updated = orderService.updateMakeStock(saved.hash)
        assertThat(updated.makeStock).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should update bid order make stock`() = runBlocking<Unit> {
        val (privateKey, _, maker) = generateNewKeys()

        val erc20Contract = AddressFactory.create()

        val order = createOrder(maker)
            .copy(
                maker = maker,
                make = Asset(Erc20AssetType(erc20Contract), EthUInt256.of(100)),
                take = Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.TEN), EthUInt256.of(100))
            )

        coEvery {
            assetMakeBalanceProvider.getMakeBalance(any())
        } returnsMany listOf(MakeBalanceState(EthUInt256.ONE), MakeBalanceState(EthUInt256.TEN))

        val saved = orderService.put(order.toForm(privateKey))
        assertThat(saved.makeStock).isEqualTo(EthUInt256.ONE)

        val updated = orderService.updateMakeStock(saved.hash)
        assertThat(updated.makeStock).isEqualTo(EthUInt256.TEN)
    }

    @Test
    fun `should fetch lazy NFT data`() = runBlocking<Unit> {
        val (privateKey, _, signer) = generateNewKeys()

        val tokenId = EthUInt256.of("${signer}000000000000000000000001")
        val randomSignature = Word(Random.nextBytes(32)).sign(privateKey)
        val erc721AssetType = Erc721AssetType(AddressFactory.create(), tokenId)
        val lazyNft = LazyErc721Dto(
            contract = erc721AssetType.token,
            tokenId = tokenId.value,
            uri = "test",
            creators = listOf(PartDto(signer, 10000)),
            royalties = emptyList(),
            signatures = listOf(randomSignature)
        )
        val erc721Order = createOrder(signer, Asset(erc721AssetType, EthUInt256.ONE))
        var lazyAssetType = Erc721LazyAssetType(
            token = erc721AssetType.token,
            tokenId = tokenId,
            uri = "test",
            creators = listOf(Part(signer, EthUInt256.Companion.of(10000L))),
            royalties = emptyList(),
            signatures = lazyNft.signatures
        )

        // set valid signature
        val nft = LazyAssetTypeToLazyNftConverter.convert(lazyAssetType)
        val hash = EIP712DomainNftFactory(BigInteger.valueOf(1))
            .createErc721Domain(erc721AssetType.token).hashToSign(nft.hash())
        lazyAssetType = lazyAssetType.copy(signatures = listOf(hash.sign(privateKey)))

        val realOrder = erc721Order.copy(make = Asset(lazyAssetType, erc721Order.make.value))
        val signature = realOrder.toForm(privateKey).signature

        val itemId = "${erc721AssetType.token}:${erc721AssetType.tokenId}"
        every { nftItemApi.getNftItemById(eq(itemId)) } returns
                Mono.just(
                    createNftItemDto(
                        erc721AssetType.token,
                        erc721AssetType.tokenId.value
                    ).copy(lazySupply = BigInteger.ONE)
                )
        every { nftItemApi.getNftLazyItemById(eq(itemId)) } returns
                Mono.just(lazyNft)
        every { nftCollectionApi.getNftCollectionById(erc721AssetType.token.hex()) } returns
                Mono.just(createNftCollectionDto(erc721AssetType.token))

        orderService.put(realOrder.toForm(privateKey).withSignature(signature))

        val orders = orderService.findOrders(
            OrderFilterSell(null, emptyList(), OrderFilterSort.LAST_UPDATE_DESC),
            10,
            null
        )

        assertThat(orders).hasSize(1)
        assertThat(orders.first().make.type)
            .isInstanceOf(Erc721LazyAssetType::class.java)
    }

    @Test
    fun `should return NFTs only orders`() = runBlocking<Unit> {
        val (privateKey, _, signer) = generateNewKeys()
        val erc20Order = createOrder(signer)
        val erc721Order =
            createOrder(signer, Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.of(1000)), EthUInt256.ONE))

        orderService.put(erc20Order.toForm(privateKey))
        orderService.put(erc721Order.toForm(privateKey))

        val orders = orderService.findOrders(
            OrderFilterSell(null, emptyList(), OrderFilterSort.LAST_UPDATE_DESC),
            10,
            null
        )

        assertThat(orders).hasSize(1)
    }

    @Test
    fun `should take origin into account`() = runBlocking<Unit> {
        val (privateKey, _, signer) = generateNewKeys()
        val origin = AddressFactory.create()
        val erc721Order1 =
            createOrder(signer, Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.of(1000)), EthUInt256.ONE))
                .copy(data = OrderRaribleV2DataV1(emptyList(), listOf(Part(origin, EthUInt256.TEN))))
        val erc721Order2 =
            createOrder(signer, Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.of(1000)), EthUInt256.ONE))

        orderService.put(erc721Order1.toForm(privateKey))
        orderService.put(erc721Order2.toForm(privateKey))

        val orders = orderService.findOrders(
            OrderFilterSell(origin, emptyList(), OrderFilterSort.LAST_UPDATE_DESC),
            10,
            null
        )

        assertThat(orders).hasSize(1)
    }

    @Test
    fun `should return NFTs orders by user`() = runBlocking<Unit> {
        val (privateKey1, _, signer1) = generateNewKeys()
        val (privateKey2, _, signer2) = generateNewKeys()
        val erc20Order = createOrder(signer1)
        val erc721Order1 =
            createOrder(signer1, Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.of(1000)), EthUInt256.ONE))
        val erc721Order2 =
            createOrder(signer2, Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.of(1001)), EthUInt256.ONE))

        orderService.put(erc20Order.toForm(privateKey1))
        orderService.put(erc721Order1.toForm(privateKey1))
        orderService.put(erc721Order2.toForm(privateKey2))

        var orders =
            orderService.findOrders(
                OrderFilterSellByMaker(
                    null,
                    emptyList(),
                    OrderFilterSort.LAST_UPDATE_DESC,
                    null,
                    listOf(signer2)
                ), 10, null
            )

        assertThat(orders).hasSize(1)

        orders =
            orderService.findOrders(
                OrderFilterSellByMaker(
                    null,
                    emptyList(),
                    OrderFilterSort.LAST_UPDATE_DESC,
                    null,
                    listOf(signer1, signer2)
                ), 10, null
            )

        assertThat(orders).hasSize(2)
    }

    @Test
    fun `should return NFTs orders by user and pagination`() = runBlocking {
        val (privateKey1, _, signer1) = generateNewKeys()
        val (privateKey2, _, signer2) = generateNewKeys()

        val erc20Order = createOrder(signer1)
        val erc721Order1 =
            createOrder(signer1, Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.of(1000)), EthUInt256.ONE))
        val erc721Order2 =
            createOrder(signer2, Asset(Erc721AssetType(AddressFactory.create(), EthUInt256.of(1001)), EthUInt256.ONE))
        val erc721Order3 =
            createOrder(signer2, Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.of(1001)), EthUInt256.ONE))
        val erc721Order4 =
            createOrder(signer2, Asset(Erc1155AssetType(AddressFactory.create(), EthUInt256.of(1001)), EthUInt256.ONE))

        orderService.put(erc20Order.toForm(privateKey1))
        orderService.put(erc721Order1.toForm(privateKey1))
        orderService.put(erc721Order2.toForm(privateKey2))
        orderService.put(erc721Order3.toForm(privateKey2))
        orderService.put(erc721Order4.toForm(privateKey2))

        Wait.waitAssert {
            val orders = orderService.findOrders(
                OrderFilterSellByMaker(null, emptyList(), OrderFilterSort.LAST_UPDATE_DESC, null, listOf(signer2)),
                10, null
            )

            assertThat(orders).hasSize(3)

            val midOrder = orders.sortedByDescending { it.lastUpdateAt }[1]
            val continuation = Continuation.LastDate(midOrder.lastUpdateAt, midOrder.hash)

            val ordersPaged = orderService.findOrders(
                OrderFilterSellByMaker(null, emptyList(), OrderFilterSort.LAST_UPDATE_DESC, null, listOf(signer2)),
                10,
                continuation.toString()
            )
            assertThat(ordersPaged).hasSize(1)
        }
    }

    @Test
    fun `should return NFTs orders by collection`() = runBlocking<Unit> {
        val (privateKey1, _, signer1) = generateNewKeys()
        val (privateKey2, _, signer2) = generateNewKeys()
        val erc20Order = createOrder(signer1)
        val collection1 = AddressFactory.create()
        val erc721Order1 =
            createOrder(signer1, Asset(Erc721AssetType(collection1, EthUInt256.of(1000)), EthUInt256.ONE))

        val collection2 = AddressFactory.create()
        val erc1155Order2 =
            createOrder(signer2, Asset(Erc1155AssetType(collection2, EthUInt256.of(1001)), EthUInt256.ONE))

        orderService.put(erc20Order.toForm(privateKey1))
        orderService.put(erc721Order1.toForm(privateKey1))
        orderService.put(erc1155Order2.toForm(privateKey2))

        val orders1 = orderService.findOrders(
            OrderFilterSellByCollection(
                null,
                emptyList(),
                OrderFilterSort.LAST_UPDATE_DESC, null,
                collection1
            ), 10, null
        )
        assertThat(orders1).hasSize(1)

        val orders2 = orderService.findOrders(
            OrderFilterSellByCollection(
                null,
                emptyList(),
                OrderFilterSort.LAST_UPDATE_DESC, null,
                collection2
            ), 10, null
        )
        assertThat(orders2).hasSize(1)
    }

    @Test
    fun `should return NFTs sell orders by item`() = runBlocking<Unit> {
        val collection1 = AddressFactory.create()
        val tokenId1 = EthUInt256.of(1000)

        val erc721Order1 = createOrder().copy(
            make = Asset(Erc721AssetType(collection1, tokenId1), EthUInt256.ONE),
            makeStock = EthUInt256.TEN
        )
        orderRepository.save(erc721Order1)

        val collection2 = AddressFactory.create()
        val tokenId2 = EthUInt256.of(1001)

        val erc1155Order2 = createOrder().copy(
            make = Asset(Erc1155AssetType(collection2, tokenId2), EthUInt256.ONE),
            makeStock = EthUInt256.TEN
        )
        orderRepository.save(erc1155Order2)

        val orders1 = orderService.findOrders(
            OrderFilterSellByItem(
                contract = collection1,
                tokenId = tokenId1.value,
                sort = OrderFilterSort.LAST_UPDATE_DESC,
                origin = null,
                platforms = emptyList(),
                maker = null
            ), 10, null
        )
        assertThat(orders1).hasSize(1)

        val orders2 = orderService.findOrders(
            OrderFilterSellByItem(
                contract = collection2,
                tokenId = tokenId2.value,
                sort = OrderFilterSort.LAST_UPDATE_DESC,
                origin = null,
                platforms = emptyList(),
                maker = null
            ), 10, null
        )
        assertThat(orders2).hasSize(1)
    }

    private suspend fun saveRandomOrderWithMakeBalance(): Order {
        val (_, _, signer) = generateNewKeys()
        val order = createOrder(signer).copy(make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.ONE))
        return orderRepository.save(order)
    }

    private suspend fun saveRandomOpenSeaOrderWithMakeBalance(): Order {
        val (_, _, signer) = generateNewKeys()
        val order =
            createOpenSeaOrder(signer).copy(make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.ONE))
        return orderRepository.save(order)
    }

    private suspend fun saveRandomCryptoPunkOrderWithMakeBalance(): Order {
        val (_, _, signer) = generateNewKeys()
        val order =
            orderCryptoPunksData(signer).copy(make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.ONE))
        return orderRepository.save(order)
    }

    private suspend fun saveRandomX2Y2OrderWithMakeBalance(): Order {
        val (_, _, signer) = generateNewKeys()
        val order =
            orderX2Y2Data(signer).copy(make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.ONE))
        return orderRepository.save(order)
    }

    private suspend fun saveRandomLooksrareOrderWithMakeBalance(): Order {
        val (_, _, signer) = generateNewKeys()
        val order =
            orderLooksrareData(signer).copy(make = Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.ONE))
        return orderRepository.save(order)
    }

    private suspend fun saveRandomOrder(): Order{
        val (_, _, signer) = generateNewKeys()
        val order = createOrder(signer)
        return orderRepository.save(order)
    }
}
