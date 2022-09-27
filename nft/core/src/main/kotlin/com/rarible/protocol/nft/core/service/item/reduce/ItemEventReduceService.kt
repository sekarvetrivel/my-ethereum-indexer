package com.rarible.protocol.nft.core.service.item.reduce

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.core.apm.withSpan
import com.rarible.core.entity.reducer.service.EventReduceService
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.converters.model.ItemEventConverter
import com.rarible.protocol.nft.core.model.ItemEvent
import com.rarible.protocol.nft.core.converters.model.ItemIdFromStringConverter
import com.rarible.protocol.nft.core.misc.asEthereumLogRecord
import com.rarible.protocol.nft.core.model.*
import org.springframework.stereotype.Component

@Component
class ItemEventReduceService(
    entityService: ItemUpdateService,
    entityIdService: ItemIdService,
    templateProvider: ItemTemplateProvider,
    reducer: ItemReducer,
    private val onNftItemLogEventListener: OnNftItemLogEventListener,
    private val itemEventConverter: ItemEventConverter,
    properties: NftIndexerProperties
) {
    private val skipTransferContractTokens = properties.scannerProperties.skipTransferContractTokens.map(ItemIdFromStringConverter::convert)
    private val delegate = EventReduceService(entityService, entityIdService, templateProvider, reducer)

    suspend fun reduce(events: List<ItemEvent>) {
        delegate.reduceAll(events)
    }

    suspend fun onEntityEvents(events: List<LogRecordEvent>) {
        withSpan(
            name = "onItemEvents",
            labels = listOf("itemId" to (events.firstOrNull()?.record?.asEthereumLogRecord()?.let { itemEventConverter.convert(it) } ?: "" ))
        ) {
            events
                .onEach { onNftItemLogEventListener.onLogEvent(it) }
                .mapNotNull { itemEventConverter.convert(it.record.asEthereumLogRecord()) }
                .filter { itemEvent -> ItemId.parseId(itemEvent.entityId) !in skipTransferContractTokens }
                .let { delegate.reduceAll(it) }
        }
    }
}
