package com.rarible.protocol.order.listener.service.descriptors.exchange.opensea

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.listener.log.LogEventDescriptor
import com.rarible.protocol.contracts.exchange.wyvern.NonceIncrementedEvent
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.ChangeNonceHistory
import com.rarible.protocol.order.core.model.HistorySource
import com.rarible.protocol.order.core.repository.nonce.NonceHistoryRepository
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactor.mono
import org.reactivestreams.Publisher
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import scalether.domain.Address
import scalether.domain.response.Log
import scalether.domain.response.Transaction
import java.math.BigInteger
import java.time.Instant

@Service
@CaptureSpan(type = SpanType.EVENT)
class WyvernExchangeChangeNoneDescriptor(
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
    private val properties: OrderIndexerProperties
) : LogEventDescriptor<ChangeNonceHistory> {

    override val collection: String
        get() = NonceHistoryRepository.COLLECTION

    override val topic: Word = NonceIncrementedEvent.id()

    override fun convert(log: Log, transaction: Transaction, timestamp: Long, index: Int, totalLogs: Int): Publisher<ChangeNonceHistory> {
        return mono { convert(log, Instant.ofEpochSecond(timestamp)) }
    }

    private suspend fun convert(log: Log, date: Instant): ChangeNonceHistory {
        val event = NonceIncrementedEvent.apply(log)
        return ChangeNonceHistory(
            maker = event.maker(),
            newNonce = EthUInt256.of(event.newNonce() + BigInteger.valueOf(properties.openSeaNonceIncrement)),
            date = date,
            source = HistorySource.OPEN_SEA
        )
    }

    override fun getAddresses(): Mono<Collection<Address>> = Mono.just(listOf(exchangeContractAddresses.openSeaV2))
}