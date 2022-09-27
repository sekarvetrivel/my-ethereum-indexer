package com.rarible.protocol.nft.core.service.item.reduce.reversed

import com.rarible.core.entity.reducer.service.Reducer
import com.rarible.protocol.nft.core.model.Item
import com.rarible.protocol.nft.core.model.ItemEvent
import org.springframework.stereotype.Component

@Component
class ReversedLazyValueItemReducer : Reducer<ItemEvent, Item> {
    override suspend fun reduce(entity: Item, event: ItemEvent): Item {
        return when (event) {
            is ItemEvent.ItemMintEvent -> {
                if (entity.isLazyItem()) {
                    entity.copy(
                        lazySupply = entity.lazySupply + event.supply,
                        supply = entity.supply + event.supply
                    )
                } else {
                    entity
                }
            }
            is ItemEvent.ItemTransferEvent,
            is ItemEvent.ItemBurnEvent,
            is ItemEvent.OpenSeaLazyItemMintEvent,
            is ItemEvent.ItemCreatorsEvent -> entity
            is ItemEvent.LazyItemBurnEvent, is ItemEvent.LazyItemMintEvent ->
                throw IllegalArgumentException("This events can't be in this reducer")
        }
    }
}
