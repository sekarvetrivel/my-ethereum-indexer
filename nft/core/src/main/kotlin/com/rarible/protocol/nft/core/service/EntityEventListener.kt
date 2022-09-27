package com.rarible.protocol.nft.core.service

import com.rarible.blockchain.scanner.framework.data.LogRecordEvent
import com.rarible.protocol.nft.core.model.SubscriberGroup

interface EntityEventListener {
    val id: String

    val subscriberGroup: SubscriberGroup

    suspend fun onEntityEvents(events: List<LogRecordEvent>)
}
