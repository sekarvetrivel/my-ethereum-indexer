package com.rarible.protocol.nft.api.e2e.collection

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.nft.api.configuration.NftIndexerApiProperties.OperatorProperties
import com.rarible.protocol.nft.api.e2e.data.createToken
import com.rarible.protocol.nft.api.service.colllection.CollectionService
import com.rarible.protocol.nft.core.model.TokenFeature
import com.rarible.protocol.nft.core.model.TokenStandard
import com.rarible.protocol.nft.core.repository.TokenIdRepository
import com.rarible.protocol.nft.core.repository.token.TokenRepository
import com.rarible.protocol.nft.core.service.token.TokenRegistrationService
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import reactor.core.publisher.Mono
import scalether.abi.Uint256Type
import scalether.domain.Address
import scalether.domain.AddressFactory
import scalether.util.Hex
import java.math.BigInteger

class CollectionServiceIt {
    private val privateKey = Numeric.toBigInt(RandomUtils.nextBytes(32))
    private val publicKey = Sign.publicKeyFromPrivate(privateKey)

    private val operator = OperatorProperties(Hex.prefixed(privateKey.toByteArray()))
    private val tokenRepository = mockk<TokenRepository>()
    private val tokenIdRepository = mockk<TokenIdRepository>()
    private val tokenRegistrationService = mockk<TokenRegistrationService>()
    private val collectionService = CollectionService(operator, mockk(), tokenRegistrationService, tokenRepository, tokenIdRepository, mockk())

    @BeforeEach
    fun setup() {
        clearMocks(tokenRepository, tokenIdRepository)
    }

    @Test
    fun testGenerateWithMinter() = runBlocking<Unit> {
        val tokenAddress = AddressParser.parse("0x6ede7f3c26975aad32a475e1021d8f6f39c89d82")//AddressFactory.create()
        val createToken = createToken()
        val token = createToken.copy(
            standard = TokenStandard.ERC1155,
            features = setOf(TokenFeature.MINT_AND_TRANSFER)
        )
        val maker = Address.apply("0x0Db5B06DE681949Cb4821e7E6246127d08549b94")

        coEvery { tokenIdRepository.generateTokenId(eq("$tokenAddress:$maker")) } returns 1L
        every { tokenRegistrationService.register(eq(tokenAddress)) } returns Mono.just(token)

        val result = collectionService.generateId(tokenAddress, maker)

        assertThat(result.tokenId).isEqualTo(EthUInt256.of("0x25646b08d9796ceda5fb8ce0105a51820740c04900000000000000000000000a"))
    }

    @Test
    fun testGenerateSignWithTokenAddress() = runBlocking<Unit> {
        val tokenAddress = AddressParser.parse("0x2fce8435f0455edc702199741411dbcd1b7606ca")//AddressFactory.create()

        val token = createToken().copy(
            id = tokenAddress,
            standard = TokenStandard.ERC1155,
            features = setOf(TokenFeature.MINT_WITH_ADDRESS, TokenFeature.MINT_AND_TRANSFER)
        )
        val maker = AddressParser.parse("0xca8ea83f10b7098e989293c0b638048d0eed38ae")//AddressFactory.create()

        val nextTokenId = BigInteger.valueOf(10)
        coEvery { tokenIdRepository.generateTokenId(eq("$tokenAddress:$maker")) } returns nextTokenId.longValueExact()
        every { tokenRegistrationService.register(eq(tokenAddress)) } returns Mono.just(token)

        val result = collectionService.generateId(tokenAddress, maker)

        val sign = Sign.signMessage(tokenAddress.add(Uint256Type.encode(result.tokenId.value)).bytes(), publicKey, privateKey)

        assertThat(result.sign.r).isEqualTo(sign.r)
        assertThat(result.sign.s).isEqualTo(sign.s)
        assertThat(result.sign.v).isEqualTo(sign.v)
    }

    @Test
    fun testGenerateWithCollection() = runBlocking<Unit> {
        val tokenAddress = AddressFactory.create()

        val token = createToken().copy(
            id = tokenAddress,
            standard = TokenStandard.ERC1155,
            features = setOf()
        )
        val maker = AddressFactory.create()

        val nextTokenId = 10L
        coEvery { tokenIdRepository.generateTokenId(eq("$tokenAddress")) } returns nextTokenId
        every { tokenRegistrationService.register(eq(tokenAddress)) } returns Mono.just(token)

        val result = collectionService.generateId(tokenAddress, maker)

        val sign = Sign.signMessage(Uint256Type.encode(result.tokenId.value).bytes(), publicKey, privateKey)

        assertThat(result.sign.r).isEqualTo(sign.r)
        assertThat(result.sign.s).isEqualTo(sign.s)
        assertThat(result.sign.v).isEqualTo(sign.v)
    }
}
