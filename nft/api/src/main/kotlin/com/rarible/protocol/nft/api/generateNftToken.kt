package com.rarible.protocol.nft.api

import com.mongodb.reactivestreams.client.MongoClients
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.nft.core.model.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import scalether.abi.Uint256Type
import scalether.domain.Address
import java.math.BigInteger


suspend fun main() {
    //collectionId address
    val collectionId: Address = AddressParser.parse("0x135bfaae22aa4d63a86ff38e0a7d22c8fc25d086")

    //minter address
    val minter: Address = AddressParser.parse("0xf731327d8d3d9c0cc71b9c6d05fc2cec4fd4dcc3")

    //token
    val token = Token(
        id = collectionId,
        owner = AddressParser.parse("0x308c30f6a120c9ef65772697fcda46711e7acec6"),
        name = "34b8f310-fed4-4263-8f55-6d829024c1f9",
        symbol = "812f7012-02b0-4062-abe3-fc2fd107a406",
        status = ContractStatus.CONFIRMED,
        features = setOf(TokenFeature.MINT_WITH_ADDRESS, TokenFeature.MINT_AND_TRANSFER),
        dbUpdatedAt = null,
        lastEventId = null,
        standard = TokenStandard.ERC1155,
        version = null,
        isRaribleContract = false,
        deleted = false,
        revertableEvents = emptyList()
    )

    //tokenIdKey contains combination of collectionId and minter address
    val tokenIdKey = "$collectionId:$minter"

    //tokenId initial value
    val tokenIdInitialValue = 0

    //if tokenIdInitialValue true its call generateTokenIdWithInitialValue methods otherwise skip that function
    val tokenIdFlag = tokenIdInitialValue > 0

    //mongo template
    val mongo: ReactiveMongoOperations = ReactiveMongoTemplate(MongoClients.create(), "test")

    //mongoDB queries for find id is exists
    val query = Query(Criteria.where("id").`is`(tokenIdKey))

    //find and Modify options
    val findAndModify = FindAndModifyOptions.options().returnNew(true).upsert(true)

    if (tokenIdFlag) {
        //mongoDB queries for initiate value
        val update = Update().setOnInsert("id", tokenIdKey).setOnInsert("value", tokenIdInitialValue)

        //calculate and update initial value using queries
        val value = mongo.findAndModify(
            query, update, findAndModify, TokenId::class.java
        ).awaitFirstOrNull()

        value?.value //just for check value while debug
    }

    //mongoDB queries for inc value
    val updateInc = Update().inc("value", 1)

    //returns long value of mongo queries result and update inc value too
    val generateTokenId = mongo.findAndModify(
        query, updateInc, findAndModify, TokenId::class.java
    ).awaitFirst().value

    //convert long value of generateTokenId to BigInteger
    val tokedIdtoBigInteger = generateTokenId.toBigInteger()

    //calculate value of biginteger 2 than make it power of 96
    val valueOfTwo = BigInteger.valueOf(2).pow(96)

    //just check value of biginteger 2 is less than
    assert(tokedIdtoBigInteger < valueOfTwo) { "tokenId size error" }

    //encode value of biginteger value
    val encode = Uint256Type.encode(tokedIdtoBigInteger)

    //slice encode value 20 to 32 value in binary
    val encoded = encode.slice(20, 32)

    //get binary value of add minter with encoded
    val binary = minter.add(encoded)

    //decode binary using Uint256Type
    val decode = Uint256Type.decode(binary, 0)

    //Convert biginteger to EthUInt256
    val nextTokenId = EthUInt256.of(decode.value())

    println("Next TokenId - " + nextTokenId)
}
