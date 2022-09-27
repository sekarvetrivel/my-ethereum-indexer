package com.rarible.protocol.order.listener.service.order

import com.rarible.core.task.TaskHandler
import com.rarible.protocol.order.core.configuration.OrderIndexerProperties
import com.rarible.protocol.order.core.model.Order
import com.rarible.protocol.order.core.model.OrderStatus
import com.rarible.protocol.order.core.model.OrderType
import com.rarible.protocol.order.core.model.Platform
import com.rarible.protocol.order.core.repository.order.OrderRepository
import com.rarible.protocol.order.core.service.OrderUpdateService
import kotlinx.coroutines.flow.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RemoveOpenSeaOutdatedOrdersTaskHandler(
    private val orderRepository: OrderRepository,
    private val orderUpdateService: OrderUpdateService,
    private val exchangeContractAddresses: OrderIndexerProperties.ExchangeContractAddresses,
) : TaskHandler<String> {

    val logger: Logger = LoggerFactory.getLogger(RemoveOpenSeaOutdatedOrdersTaskHandler::class.java)

    override val type: String
        get() = REMOVE_OPEN_SEA_OUTDATED_ORDERS

    override fun runLongTask(from: String?, param: String): Flow<String> {
        val status = OrderStatus.valueOf(param)
        logger.info("Start $REMOVE_OPEN_SEA_OUTDATED_ORDERS task with $status param")
        return orderRepository
            .findAll(Platform.OPEN_SEA, status, fromHash = null)
            .filter { isOpenSeaOrderType(it) }
            .map { updateOrder(it) }
    }

    private fun isOpenSeaOrderType(order: Order): Boolean {
        return order.type == OrderType.OPEN_SEA_V1
    }

    private suspend fun updateOrder(order: Order): String  {
        orderUpdateService.update(order.hash)
        return order.hash.toString()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RemoveOpenSeaOutdatedOrdersTaskHandler::class.java)
        const val REMOVE_OPEN_SEA_OUTDATED_ORDERS = "REMOVE_OPEN_SEA_OUTDATED_ORDERS"
    }
}