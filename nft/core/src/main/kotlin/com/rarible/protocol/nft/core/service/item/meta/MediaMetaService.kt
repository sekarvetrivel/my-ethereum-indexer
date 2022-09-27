package com.rarible.protocol.nft.core.service.item.meta

import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.service.UrlService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component

@Deprecated(
    "Remove this class when Mulitchain API will take over Ethereum API. " +
            "Loading of metadata is performed on the Multichain API."
)
@Component
class MediaMetaService(
    private val urlService: UrlService,
    private val template: ReactiveMongoTemplate
) {

    suspend fun getMediaMetaFromCache(url: String, id: String): ContentMeta? {
        // For cache, if we still use it, lets use mypinata urls, as previously
        if (url.isBlank()) {
            return null
        }
        val realUrl = urlService.resolvePublicHttpUrl(url) ?: return null
        return fetchFromCache(realUrl)
    }

    private suspend fun fetchFromCache(url: String): ContentMeta? {
        for (candidateUrl in getCandidateUrls(url)) {
            val cacheEntry = template.findById<CachedContentMetaEntry>(
                id = candidateUrl,
                collectionName = CachedContentMetaEntry.CACHE_META_COLLECTION
            ).awaitFirstOrNull()
            if (cacheEntry != null) {
                cacheEntry.data ?: run { return null }
                return cacheEntry.data.let {
                    ContentMeta(
                        type = it.type,
                        width = it.width,
                        height = it.height,
                        size = it.size
                    )
                }
            }
        }
        return null
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getCandidateUrls(url: String): List<String> =
        buildList {
            add(url)
            val hash = getIpfsHash(url)
            if (hash != null) {
                ipfsPrefixes.mapTo(this) { it + hash }
            }
        }.distinct()

    private fun getIpfsHash(url: String): String? {
        for (prefix in ipfsPrefixes) {
            if (url.startsWith(prefix)) {
                return url.substringAfter(prefix)
            }
        }
        return null
    }

    companion object {

        private const val rariblePinata = "https://rarible.mypinata.cloud/ipfs/"
        private const val ipfsRarible = "https://ipfs.rarible.com/ipfs/"
        private val ipfsPrefixes = listOf(rariblePinata, ipfsRarible)
    }
}
