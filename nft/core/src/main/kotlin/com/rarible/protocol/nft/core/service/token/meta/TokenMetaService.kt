package com.rarible.protocol.nft.core.service.token.meta

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.nft.core.model.TokenMeta
import com.rarible.protocol.nft.core.model.TokenMetaContent
import com.rarible.protocol.nft.core.service.item.meta.MediaMetaService
import com.rarible.protocol.nft.core.service.item.meta.TOKEN_META_CAPTURE_SPAN_TYPE
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder
import com.rarible.protocol.nft.core.service.item.meta.properties.ContentBuilder.cleanUrl
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import scalether.domain.Address

@Component
@CaptureSpan(type = TOKEN_META_CAPTURE_SPAN_TYPE)
class TokenMetaService(
    private val tokenPropertiesService: TokenPropertiesService,
    private val mediaMetaService: MediaMetaService
) {

    suspend fun get(id: Address): TokenMeta {
        val properties = tokenPropertiesService.resolve(id)

        val imageOriginal = properties?.content?.imageOriginal

        if (properties == null || imageOriginal == null || imageOriginal.url.cleanUrl() == null) return TokenMeta.EMPTY

        mediaMetaService.getMediaMetaFromCache(imageOriginal.url, id.prefixed())
        return TokenMeta(
            properties = properties.copy(
                content = TokenMetaContent(
                    ContentBuilder.populateContent(
                        ethMetaContent = imageOriginal,
                        data = mediaMetaService.getMediaMetaFromCache(imageOriginal.url, id.prefixed())
                    )
                )
            )
        )
    }

    suspend fun reset(id: Address) {
        tokenPropertiesService.reset(id)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(TokenMetaService::class.java)
    }
}
