package com.rarible.protocol.order.core.service

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomWord
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.domain.LogEvent
import com.rarible.ethereum.listener.log.domain.LogEventStatus
import com.rarible.protocol.order.core.data.*
import com.rarible.protocol.order.core.integration.AbstractIntegrationTest
import com.rarible.protocol.order.core.integration.IntegrationTest
import com.rarible.protocol.order.core.model.*
import io.daonomic.rpc.domain.Word
import io.daonomic.rpc.domain.WordFactory
import kotlinx.coroutines.cancel
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.time.Duration

@IntegrationTest
class OrderReduceServiceIt : AbstractIntegrationTest() {

    @Test
    fun `should calculate order for existed order`() = runBlocking<Unit> {
        val order = createOrderVersion()
        orderUpdateService.save(order)

        val sideMatchDate1 = nowMillis() + Duration.ofHours(2)
        val sideMatchDate2 = nowMillis() + Duration.ofHours(1)
        val cancelDate = nowMillis() + Duration.ofHours(3)

        prepareStorage(
            OrderSideMatch(
                hash = order.hash,
                counterHash = WordFactory.create(),
                fill = EthUInt256.of(1),
                make = order.make,
                take = order.take,
                maker = order.maker,
                side = OrderSide.LEFT,
                taker = Address.FOUR(),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = priceNormalizer.normalize(order.make),
                takeValue = priceNormalizer.normalize(order.take),
                date = sideMatchDate1,
                source = HistorySource.RARIBLE
            ),
            OrderSideMatch(
                hash = order.hash,
                counterHash = WordFactory.create(),
                fill = EthUInt256.of(2),
                maker = order.maker,
                make = order.make,
                take = order.take,
                side = OrderSide.LEFT,
                taker = Address.FOUR(),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = priceNormalizer.normalize(order.make),
                takeValue = priceNormalizer.normalize(order.take),
                date = sideMatchDate2,
                source = HistorySource.RARIBLE
            ),
            OrderCancel(
                hash = order.hash,
                maker = order.maker,
                make = order.make,
                take = order.take,
                date = cancelDate,
                source = HistorySource.RARIBLE
            )
        )
        val result = orderReduceService.updateOrder(order.hash)!!

        assertThat(result.fill).isEqualTo(EthUInt256.of(3))
        assertThat(result.cancelled).isEqualTo(true)
        assertThat(result.lastUpdateAt).isEqualTo(cancelDate)
    }

    @Test
    fun `should not change order lastUpdateAt if reduce past events`() = runBlocking<Unit> {
        val orderVersion = createOrderVersion()
        val orderCreatedAt = orderVersion.createdAt

        orderUpdateService.save(orderVersion)

        prepareStorage(
            OrderCancel(
                hash = orderVersion.hash,
                maker = orderVersion.maker,
                make = orderVersion.make,
                take = orderVersion.take,
                date = orderVersion.createdAt - Duration.ofHours(1),
                source = HistorySource.RARIBLE
            )
        )
        orderReduceService.updateOrder(orderVersion.hash)

        val updatedOrder = orderRepository.findById(orderVersion.hash)
        assertThat(updatedOrder?.cancelled).isEqualTo(true)
        assertThat(updatedOrder?.lastUpdateAt).isEqualTo(orderCreatedAt)
    }

    @Test
    fun `should not change lastEventId with only orderVersions`() = runBlocking<Unit> {
        val hash = Word.apply(randomWord())

        prepareStorage(
            createOrderVersion().copy(hash = hash),
            createOrderVersion().copy(hash = hash),
            createOrderVersion().copy(hash = hash)
        )

        val order = orderReduceService.updateOrder(hash)!!
        assertThat(order.lastEventId).isNotNull()

        val recalculatedOrder = orderReduceService.updateOrder(hash)!!
        assertThat(recalculatedOrder.lastEventId).isEqualTo(order.lastEventId)
    }


    @Test
    fun `should not change lastEventId with orderVersions and logEvents`() = runBlocking<Unit> {
        val hash = Word.apply(randomWord())

        prepareStorage(
            createOrderVersion().copy(hash = hash),
            createOrderVersion().copy(hash = hash),
            createOrderVersion().copy(hash = hash)
        )
        prepareStorage(
            createOrderSideMatch().copy(hash = hash),
            createOrderCancel().copy(hash = hash)
        )

        val order = orderReduceService.updateOrder(hash)!!
        assertThat(order.lastEventId).isNotNull()

        val recalculatedOrder = orderReduceService.updateOrder(hash)!!
        assertThat(recalculatedOrder.lastEventId).isEqualTo(order.lastEventId)
    }

    @Test
    fun `should change lastEventId if a new event come`() = runBlocking<Unit> {
        val hash = Word.apply(randomWord())

        prepareStorage(
            createOrderVersion().copy(hash = hash)
        )
        prepareStorage(
            createOrderSideMatch().copy(hash = hash)
        )

        val order = orderReduceService.updateOrder(hash)!!
        assertThat(order.lastEventId).isNotNull()

        prepareStorage(
            createOrderCancel().copy(hash = hash)
        )

        val updatedOrder = orderReduceService.updateOrder(hash)!!
        assertThat(updatedOrder.lastEventId).isNotNull()
        assertThat(updatedOrder.lastEventId).isNotEqualTo(order.lastEventId)
    }

    @Test
    internal fun `take of order version was updated`() = runBlocking<Unit> {
        val make = Asset(Erc721AssetType(randomAddress(), EthUInt256.of(42)), EthUInt256.ONE)
        val take = Asset(Erc20AssetType(randomAddress()), EthUInt256.of(10))
        val orderVersion = createOrderVersion().copy(make = make, take = take)
        val hash = orderVersion.hash
        val saved = orderUpdateService.save(orderVersion)
        assertThat(saved.take.value).isEqualTo(take.value)
        assertThat(orderRepository.findById(hash)?.take?.value).isEqualTo(take.value)
        val newTakeValue = EthUInt256.Companion.of(5)
        val updated = orderUpdateService.save(
            orderVersion.copy(
                take = orderVersion.take.copy(value = newTakeValue)
            )
        )
        assertThat(updated.take.value).isEqualTo(newTakeValue)
        assertThat(orderRepository.findById(hash)?.take?.value).isEqualTo(newTakeValue)
    }

    @Test
    internal fun `should cancel OpenSea order if nonces are not matched`() = runBlocking<Unit> {
        val now = nowMillis()
        val data = createOrderOpenSeaV1DataV1().copy(nonce = 0)
        val orderVersion = createOrderVersion().copy(
            type = OrderType.OPEN_SEA_V1,
            data = data,
            createdAt = now
        )
        val hash = orderVersion.hash
        orderUpdateService.save(orderVersion)

        val nonce = ChangeNonceHistory(
            maker = orderVersion.maker,
            newNonce = EthUInt256.ONE,
            date = now + Duration.ofMinutes(10),
            source = HistorySource.OPEN_SEA
        )
        nonceHistoryRepository.save(
            LogEvent(
                data = nonce,
                address = data.exchange,
                topic = Word.apply(randomWord()),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0,
                createdAt = now,
                updatedAt = now
            )
        )
        val updated = orderReduceService.updateOrder(hash)!!
        assertThat(updated.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(updated.lastUpdateAt).isEqualTo(nonce.date)
    }

    @Test
    internal fun `should cancel Seaport order if counters are not matched`() = runBlocking<Unit> {
        val now = nowMillis()
        val data = createOrderBasicSeaportDataV1().copy(counter = 0)
        val orderVersion = createOrderVersion().copy(
            type = OrderType.SEAPORT_V1,
            data = data,
            createdAt = now
        )
        val hash = orderVersion.hash
        orderUpdateService.save(orderVersion)

        val nonce = ChangeNonceHistory(
            maker = orderVersion.maker,
            newNonce = EthUInt256.ONE,
            date = now + Duration.ofMinutes(10),
            source = HistorySource.OPEN_SEA
        )
        nonceHistoryRepository.save(
            LogEvent(
                data = nonce,
                address = data.protocol,
                topic = Word.apply(randomWord()),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0,
                createdAt = now,
                updatedAt = now
            )
        )
        val updated = orderReduceService.updateOrder(hash)!!
        assertThat(updated.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(updated.lastUpdateAt).isEqualTo(nonce.date)
    }

    @Test
    @Deprecated("Remove with OpenSea order specific code")
    internal fun `should not cancel OpenSea order if nonces are matched`() = runBlocking<Unit> {
        val now = nowMillis()
        val data = createOrderOpenSeaV1DataV1().copy(nonce = 1)
        val orderVersion = createOrderVersion().copy(
            type = OrderType.OPEN_SEA_V1,
            data = data,
            createdAt = now
        )
        val hash = orderVersion.hash
        orderUpdateService.save(orderVersion)

        val nonce = ChangeNonceHistory(
            maker = orderVersion.maker,
            newNonce = EthUInt256.ONE,
            date = now + Duration.ofMinutes(10),
            source = HistorySource.OPEN_SEA
        )
        nonceHistoryRepository.save(
            LogEvent(
                data = nonce,
                address = data.exchange,
                topic = Word.apply(randomWord()),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0,
                createdAt = now,
                updatedAt = now
            )
        )
        val updated = orderReduceService.updateOrder(hash)!!
        assertThat(updated.status).isEqualTo(OrderStatus.CANCELLED)
        assertThat(updated.lastUpdateAt).isEqualTo(orderVersion.createdAt)
    }

    @Test
    internal fun `should not cancel Seaport order if counter are matched`() = runBlocking<Unit> {
        val now = nowMillis()
        val data = createOrderBasicSeaportDataV1().copy(counter = 1)
        val orderVersion = createOrderVersion().copy(
            type = OrderType.SEAPORT_V1,
            data = data,
            createdAt = now
        )
        val hash = orderVersion.hash
        orderUpdateService.save(orderVersion)

        val nonce = ChangeNonceHistory(
            maker = orderVersion.maker,
            newNonce = EthUInt256.ONE,
            date = now + Duration.ofMinutes(10),
            source = HistorySource.OPEN_SEA
        )
        nonceHistoryRepository.save(
            LogEvent(
                data = nonce,
                address = data.protocol,
                topic = Word.apply(randomWord()),
                transactionHash = Word.apply(randomWord()),
                status = LogEventStatus.CONFIRMED,
                blockNumber = 1,
                logIndex = 0,
                minorLogIndex = 0,
                index = 0,
                createdAt = now,
                updatedAt = now
            )
        )
        val updated = orderReduceService.updateOrder(hash)!!
        assertThat(updated.status).isNotEqualTo(OrderStatus.CANCELLED)
        assertThat(updated.lastUpdateAt).isEqualTo(orderVersion.createdAt)
    }

    @Test
    internal fun `should cancel OpenSea order as it turned off on prod`() = runBlocking<Unit> {
        val now = nowMillis()
        val orderVersion = createOrderVersion().copy(
            data = createOrderOpenSeaV1DataV1(),
            type = OrderType.OPEN_SEA_V1,
            createdAt = now
        )
        val savedOrder = orderUpdateService.save(orderVersion)
        assertThat(savedOrder.status).isEqualTo(OrderStatus.CANCELLED)
    }

    @Test
    internal fun `should not cancel order except OpenSea`() = runBlocking<Unit> {
        OrderType.values().filter { it != OrderType.OPEN_SEA_V1 }.forEach {
            val orderVersion = createOrderVersion().copy(
                type = it,
                data = when (it) {
                    OrderType.RARIBLE_V1 -> createOrderDataLegacy()
                    OrderType.RARIBLE_V2 -> createOrderRaribleV1DataV3Sell()
                    OrderType.SEAPORT_V1 -> createOrderBasicSeaportDataV1().copy(counter = 0)
                    OrderType.CRYPTO_PUNKS -> OrderCryptoPunksData
                    OrderType.LOOKSRARE -> createOrderLooksrareDataV1()
                    OrderType.X2Y2 -> createOrderX2Y2DataV1()
                    OrderType.OPEN_SEA_V1 -> throw IllegalArgumentException("Illegal order data for this test")
                },
                createdAt = nowMillis()
            )
            val savedOrder = orderUpdateService.save(orderVersion)
            assertThat(savedOrder.status).isNotEqualTo(OrderStatus.CANCELLED)
        }
    }

    @Test
    fun `should set FILLED status for LooksRare erc721 sell order`() = runBlocking<Unit> {
        val order = createOrderVersion().copy(
            hash = Word.apply(randomWord()),
            type = OrderType.LOOKSRARE,
            platform = Platform.LOOKSRARE,
            data = createOrderLooksrareDataV1(),
            make = randomErc721(),
            take = randomErc20(EthUInt256.TEN)
        )
        orderUpdateService.save(order)

        prepareStorage(
            OrderSideMatch(
                hash = order.hash,
                counterHash = WordFactory.create(),
                fill = EthUInt256.ONE,
                make = order.make,
                take = order.take,
                maker = order.maker,
                side = OrderSide.LEFT,
                taker = Address.FOUR(),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = priceNormalizer.normalize(order.make),
                takeValue = priceNormalizer.normalize(order.take),
                source = HistorySource.LOOKSRARE
            )
        )
        val result = orderReduceService.updateOrder(order.hash)!!
        assertThat(result.fill).isEqualTo(EthUInt256.ONE)
        assertThat(result.status).isEqualTo(OrderStatus.FILLED)
    }

    @Test
    fun `should set FILLED status for LooksRare erc1155 sell order`() = runBlocking<Unit> {
        val order = createOrderVersion().copy(
            hash = Word.apply(randomWord()),
            type = OrderType.LOOKSRARE,
            platform = Platform.LOOKSRARE,
            data = createOrderLooksrareDataV1(),
            make = randomErc1155(EthUInt256.ONE),
            take = randomErc20(EthUInt256.TEN)
        )
        orderUpdateService.save(order)

        prepareStorage(
            OrderSideMatch(
                hash = order.hash,
                counterHash = WordFactory.create(),
                fill = EthUInt256.ONE,
                make = order.make,
                take = order.take,
                maker = order.maker,
                side = OrderSide.LEFT,
                taker = Address.FOUR(),
                makeUsd = null,
                takeUsd = null,
                makePriceUsd = null,
                takePriceUsd = null,
                makeValue = priceNormalizer.normalize(order.make),
                takeValue = priceNormalizer.normalize(order.take),
                source = HistorySource.LOOKSRARE
            )
        )
        val result = orderReduceService.updateOrder(order.hash)!!
        assertThat(result.fill).isEqualTo(EthUInt256.ONE)
        assertThat(result.status).isEqualTo(OrderStatus.FILLED)
    }

    private suspend fun prepareStorage(vararg histories: OrderExchangeHistory) {
        histories.forEachIndexed { index, history ->
            exchangeHistoryRepository.save(
                LogEvent(
                    data = history,
                    address = AddressFactory.create(),
                    topic = Word.apply(randomWord()),
                    transactionHash = Word.apply(randomWord()),
                    status = LogEventStatus.CONFIRMED,
                    blockNumber = 1,
                    logIndex = 0,
                    minorLogIndex = 0,
                    index = index,
                    createdAt = history.date,
                    updatedAt = history.date
                )
            ).awaitFirst()
        }
    }

    private suspend fun prepareStorage(vararg orderVersions: OrderVersion) {
        orderVersions.forEach {
            orderVersionRepository.save(it).awaitFirst()
        }
    }
}
