package com.rarible.protocol.order.core.model.order

import com.rarible.protocol.dto.OrderStatusDto
import com.rarible.protocol.dto.PlatformDto
import com.rarible.protocol.order.core.repository.order.OrderRepositoryIndexes
import org.bson.Document
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import scalether.domain.Address

data class OrderFilterSellByMaker(
    override val origin: Address? = null,
    override val platforms: List<PlatformDto>,
    override val sort: OrderFilterSort,
    override val status: List<OrderStatusDto>? = null,
    val makers: List<Address> = emptyList()
) : OrderFilter() {

    override fun toQuery(continuation: String?, limit: Int): Query {
        return Query(
            Criteria()
                .sell()
                .forMakers(makers)
                .forPlatform(platforms.mapNotNull { convert(it) })
                .fromOrigin(origin)
                .forStatus(status)
                .scrollTo(continuation, sort)
        ).limit(limit).with(sort(sort)).withHint(hint())
    }

    private fun hint(): Document {
        return OrderRepositoryIndexes.SELL_ORDERS_BY_MAKER_DEFINITION.indexKeys
    }
}
