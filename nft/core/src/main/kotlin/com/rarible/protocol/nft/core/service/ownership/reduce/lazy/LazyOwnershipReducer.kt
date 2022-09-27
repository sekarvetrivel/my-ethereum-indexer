package com.rarible.protocol.nft.core.service.ownership.reduce.lazy

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.nft.core.model.Ownership
import com.rarible.protocol.nft.core.model.OwnershipEvent
import org.springframework.stereotype.Component

@Component
class LazyOwnershipReducer : Reducer<OwnershipEvent, Ownership> {
    override suspend fun reduce(entity: Ownership, event: OwnershipEvent): Ownership {
        return if (entity.lastLazyEventTimestamp != null && entity.lastLazyEventTimestamp >= event.timestamp) {
            entity
        } else {
            val (lazyValue, value) = when (event) {
                is OwnershipEvent.LazyTransferToEvent -> {
                    val lazyValue = entity.lazyValue + event.value
                    lazyValue to lazyValue
                }
                is OwnershipEvent.LazyBurnEvent -> {
                    EthUInt256.ZERO to entity.value - entity.lazyValue
                }
                is OwnershipEvent.TransferToEvent,
                is OwnershipEvent.ChangeLazyValueEvent,
                is OwnershipEvent.TransferFromEvent -> {
                    throw IllegalArgumentException("This events can't be in this reducer")
                }
            }
            entity.copy(
                lazyValue = lazyValue,
                value = value,
                lastLazyEventTimestamp = event.timestamp
            )
        }
    }
}
