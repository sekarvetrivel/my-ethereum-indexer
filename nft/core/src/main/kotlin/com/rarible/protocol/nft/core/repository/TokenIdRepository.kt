package com.rarible.protocol.nft.core.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.model.TokenId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Component

@Component
@CaptureSpan(type = SpanType.DB)
class TokenIdRepository(
    private val mongo: ReactiveMongoOperations,
    collectionProperties: NftIndexerProperties.CollectionProperties
) {

    private val tokenIdInitialValue = collectionProperties.tokenIdInitialValue

    suspend fun save(tokenId: TokenId): TokenId {
        return mongo.save(tokenId).awaitFirst()
    }

    suspend fun generateTokenId(id: String): Long {
        // There is no need to insert base value to DB if initialValue = 0,
        // inc() operator will work for non-existing entities
        val token = tokenIdInitialValue
        println("tokenId Initial Value - "+token)
        val tokenIdFlag = tokenIdInitialValue > 0
        if (tokenIdFlag) {
            val genTokenWithInitial = generateTokenIdWithInitialValue(id)
            println("generate TokenId With Initial Value - "+genTokenWithInitial.toString())
        }

        val query = Query(Criteria.where("id").`is`(id))
        val update = Update().inc("value", 1)
        val findAndModify = FindAndModifyOptions.options().returnNew(true).upsert(true)

        val value = mongo.findAndModify(
            query,
            update,
            findAndModify,
            TokenId::class.java
        ).awaitFirst().value

        return value
    }

    // In some testnets we start counting token ID's not from 0,
    // in such case we need to generate initial value before using inc()
    private suspend fun generateTokenIdWithInitialValue(id: String) {
        val query = Query(Criteria.where("id").`is`(id))
        val update = Update().setOnInsert("id", id).setOnInsert("value", tokenIdInitialValue)
        println("tokenId value in insert - "+ tokenIdInitialValue)
        val findAndModify = FindAndModifyOptions.options().returnNew(true).upsert(true)

        val value = mongo.findAndModify(
            query,
            update,
            findAndModify,
            TokenId::class.java
        ).awaitFirstOrNull()

        value?.value
    }
}
