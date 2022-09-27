package com.rarible.protocol.nft.core.repository

import com.rarible.protocol.nft.core.configuration.NftIndexerProperties
import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.TokenId
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@IntegrationTest
class TokenIdRepositoryIt : AbstractIntegrationTest() {

    private val zeroInitValue = NftIndexerProperties.CollectionProperties(0)
    private val nonZeroInitValue = NftIndexerProperties.CollectionProperties(100)

    @Test
    fun `new token id - 0 initial value`() = runBlocking<Unit> {
        val tokenIdRepository = TokenIdRepository(mongo, zeroInitValue)
        val newTokenId = tokenIdRepository.generateTokenId("0xa7e550df5593049dcf7f8088d0272c7d8f353cd2:0xca8eA83f10b7098e989293C0B638048D0EED38Ae")
        assertThat(newTokenId)
            .isEqualTo(1L)
        val newTokenIdAgain = tokenIdRepository.generateTokenId("0x408ca22df1ed41239583eeef5cdc32383224afdb:0xca8eA83f10b7098e989293C0B638048D0EED38Ae")
        assertThat(newTokenIdAgain)
            .isEqualTo(2L)
    }

    @Test
    fun `existing token id - 0 initial value`() = runBlocking<Unit> {
        val tokenIdRepository = TokenIdRepository(mongo, zeroInitValue)
        mongo.save(TokenId("0x27b71b46b44d5972cb2ca4929e25147abd9c040d:0x25646b08d9796ceda5fb8ce0105a51820740c049", 10)).awaitFirst()
        val newTokenId = tokenIdRepository.generateTokenId("0x27b71b46b44d5972cb2ca4929e25147abd9c040d:0x25646b08d9796ceda5fb8ce0105a51820740c049")
        assertThat(newTokenId)
            .isEqualTo(11L)
        val newTokenIdAgain = tokenIdRepository.generateTokenId("0x27b71b46b44d5972cb2ca4929e25147abd9c040d:0x25646b08d9796ceda5fb8ce0105a51820740c049")
        assertThat(newTokenIdAgain)
            .isEqualTo(12L)
    }

    @Test
    fun `new token id - 100 initial value`() = runBlocking<Unit> {
        val tokenIdRepository = TokenIdRepository(mongo, nonZeroInitValue)
        val newTokenId = tokenIdRepository.generateTokenId("0x27b71b46b44d5972cb2ca4929e25147abd9c040d:0x25646b08d9796ceda5fb8ce0105a51820740c049")
        assertThat(newTokenId)
            .isEqualTo(101L)
        val newTokenIdAgain = tokenIdRepository.generateTokenId("0x27b71b46b44d5972cb2ca4929e25147abd9c040d:0x25646b08d9796ceda5fb8ce0105a51820740c049")
        assertThat(newTokenIdAgain)
            .isEqualTo(102L)
    }

    @Test
    fun `existing token id - 100 initial value`() = runBlocking<Unit> {
        val tokenIdRepository = TokenIdRepository(mongo, nonZeroInitValue)
        mongo.save(TokenId("exists", 10)).awaitFirst()
        val newTokenId = tokenIdRepository.generateTokenId("exists")
        // Should not affect existing values
        assertThat(newTokenId)
            .isEqualTo(11L)
        val newTokenIdAgain = tokenIdRepository.generateTokenId("exists")
        // Should not affect existing values
        assertThat(newTokenIdAgain)
            .isEqualTo(12L)
    }
}
