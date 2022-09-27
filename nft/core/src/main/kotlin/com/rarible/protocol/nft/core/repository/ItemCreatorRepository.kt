package com.rarible.protocol.nft.core.repository

import com.rarible.protocol.nft.core.model.ItemCreator
import com.rarible.protocol.nft.core.model.ItemId
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class ItemCreatorRepository(private val mongo: ReactiveMongoOperations) {
    fun findById(id: ItemId): Mono<ItemCreator> = mongo.findById(id)

    fun save(entity: ItemCreator): Mono<ItemCreator> = mongo.save(entity)
}
