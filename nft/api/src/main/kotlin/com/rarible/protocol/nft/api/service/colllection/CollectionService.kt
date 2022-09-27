package com.rarible.protocol.nft.api.service.colllection

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.NftCollectionDto
import com.rarible.protocol.nft.api.configuration.NftIndexerApiProperties.OperatorProperties
import com.rarible.protocol.nft.api.exceptions.EntityNotFoundApiException
import com.rarible.protocol.nft.core.converters.dto.ExtendedCollectionDtoConverter
import com.rarible.protocol.nft.core.model.ContractStatus
import com.rarible.protocol.nft.core.model.ExtendedToken
import com.rarible.protocol.nft.core.model.SignedTokenId
import com.rarible.protocol.nft.core.model.Token
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenFilter
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenIdRepository
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import com.rarible.protocol.nft.core.service.token.meta.TokenMetaService
import io.daonomic.rpc.domain.Bytes
import java.math.BigInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import scalether.abi.Uint256Type
import scalether.domain.Address
import scalether.util.Hex

@Component
class CollectionService(
    operator: OperatorProperties,
    private val collectionDtoConverter: ExtendedCollectionDtoConverter,
    private val tokenRegistrationService: TokenRegistrationService,
    private val tokenRepository: TokenRepository,
    private val tokenIdRepository: TokenIdRepository,
    private val tokenMetaService: TokenMetaService
) {
    private val logger = LoggerFactory.getLogger(CollectionService::class.java)
    private val operatorPrivateKey = Numeric.toBigInt(Hex.toBytes(operator.privateKey))
    private val operatorPublicKey = Sign.publicKeyFromPrivate(operatorPrivateKey)

    suspend fun get(collectionId: Address): NftCollectionDto {
        val token = tokenRepository.findById(collectionId).awaitFirstOrNull()
            ?.takeIf { it.standard != TokenStandard.NONE && it.status != ContractStatus.ERROR }
            ?: throw EntityNotFoundApiException("Collection", collectionId)
        return collectionDtoConverter.convert(ExtendedToken(token, tokenMetaService.get(collectionId)))
    }

    suspend fun get(ids: List<Address>): List<NftCollectionDto> {
        val tokens = tokenRepository.findByIds(ids).toList()
        val enriched = enrichWithMeta(tokens)
        return enriched.map { collectionDtoConverter.convert(it) }
    }

    suspend fun resetMeta(collectionId: Address) {
        logger.info("Refreshing collection meta by $collectionId")
        tokenMetaService.reset(collectionId)
    }

    suspend fun search(filter: TokenFilter): List<ExtendedToken> {
        val tokens = tokenRepository.search(filter).collectList().awaitFirst()
        return enrichWithMeta(tokens)
    }

    suspend fun generateId(collectionId: Address, minter: Address): SignedTokenId {
        val token = tokenRegistrationService
            .register(collectionId).awaitFirstOrNull()
            ?: throw EntityNotFoundApiException("Collection", collectionId)

        val hasMintAndTransferFeature = token.features.contains(TokenFeature.MINT_AND_TRANSFER)
        val tokenIdKey = if (hasMintAndTransferFeature) "$collectionId:$minter" else "$collectionId"


        val generateTokenId = tokenIdRepository.generateTokenId(tokenIdKey)
        println("generate TokenId - "+generateTokenId)
        val tokedIdtoBigInteger = generateTokenId
            .toBigInteger()
        val nextTokenId = tokedIdtoBigInteger
            .generateUint256(minter.takeIf { hasMintAndTransferFeature })

        println("Next TokenId - "+nextTokenId)

        val sign = sign(token, nextTokenId)
        return SignedTokenId(nextTokenId, sign)
    }

    private fun BigInteger.generateUint256(minter: Address?): EthUInt256 {
        val valueOfTwo = BigInteger.valueOf(2).pow(96)
        assert(this < valueOfTwo) { "tokenId size error" }
        val encode = Uint256Type.encode(this)
        val encoded = encode.slice(20, 32)

        val binary = if (minter != null) {
            minter.add(encoded)
        } else {
            encoded
        }

        val decode = Uint256Type.decode(binary, 0)
        val value = EthUInt256.of(decode.value())

        decodeEthUInt256(value)
        return value
    }

    private fun decodeEthUInt256(ethUInt256: EthUInt256) {
        val value = ethUInt256.value

        val big = value.shiftRight(96)

        val decode = Uint256Type.encode(big).slice(12,32)

        val binary = Uint256Type.encode(value)

        val decoded = binary.slice(0, 20)

        println(decoded)
    }

    private fun sign(token: Token, nextTokenId: EthUInt256): Sign.SignatureData {
        val address = if (token.features.contains(TokenFeature.MINT_WITH_ADDRESS)) token.id else null
        return sign(nextTokenId.value, address)
    }

    fun sign(value: BigInteger, address: Address? = null): Sign.SignatureData {
        val toSign = if (address != null) {
            address.add(Uint256Type.encode(value))
        } else {
            Uint256Type.encode(value)
        }
        return Sign.signMessage(toSign.bytes(), operatorPublicKey, operatorPrivateKey)
    }

    private suspend fun enrichWithMeta(tokens: List<Token>): List<ExtendedToken> = coroutineScope {
        tokens.map { token ->
            async {
                val meta = tokenMetaService.get(token.id)
                ExtendedToken(token, meta)
            }
        }.awaitAll()
    }
}
