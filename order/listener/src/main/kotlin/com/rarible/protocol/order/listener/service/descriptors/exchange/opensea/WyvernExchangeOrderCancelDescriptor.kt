package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.wyvern.OrderCancelledEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.OrderCancel
import com.rarible.protocol.order.core.repository.exchange.ExchangeHistoryRepository
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderEventConverter
import com.rarible.protocol.order.listener.service.opensea.OpenSeaOrderParser
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toFlux
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class WyvernExchangeOrderCancelDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val openSeaOrderEventConverter: OpenSeaOrderEventConverter,
    private val openSeaOrderParser: OpenSeaOrderParser
) : LogEventDescriptor<OrderCancel> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override val collection: String
        get() = ExchangeHistoryRepository.COLLECTION

    override val topic: Word = OrderCancelledEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<OrderCancel> {
        return mono { convert(log, transaction, Instant.ofEpochSecond(timestamp)) }.flatMapMany { it.toFlux() }
    }

    private suspend fun convert(log: Log, transaction: Transaction, date: Instant): List<OrderCancel> {
        val transactionHash =  log.transactionHash()
        val event = OrderCancelledEvent.apply(log)
        logger.info("Got OrderCancel event, tx=$transactionHash")

        val order = openSeaOrderParser.parseCancelOrder(transaction.input())
        return if (order != null) {
            openSeaOrderEventConverter.convert(order, date, event, log.address() == exchangeContractAddresses.openSeaV2)
        } else {
            logger.warn("Can't parser OpenSea cancel transaction ${transaction.value()}")
            emptyList()
        }
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(
        listOf(exchangeContractAddresses.openSeaV1, exchangeContractAddresses.openSeaV2)
    )
}
