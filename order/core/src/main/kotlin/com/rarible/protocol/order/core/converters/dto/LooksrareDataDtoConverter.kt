package com.rarible.protocol.order.core.converters.dto

import com.rarible.protocol.dto.OrderLooksRareDataV1Dto
import com.rarible.protocol.order.core.model.OrderLooksrareDataV1

object LooksrareDataDtoConverter {
    fun convert(source: OrderLooksrareDataV1): OrderLooksRareDataV1Dto {
        return OrderLooksRareDataV1Dto(
            minPercentageToAsk = source.minPercentageToAsk,
            nonce = source.counter,
            params = source.params,
            strategy = source.strategy
        )
    }
}