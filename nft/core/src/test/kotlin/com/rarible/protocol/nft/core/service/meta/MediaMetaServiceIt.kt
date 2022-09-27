package com.rarible.protocol.nft.core.service.meta

import com.rarible.protocol.nft.core.integration.AbstractIntegrationTest
import com.rarible.protocol.nft.core.integration.IntegrationTest
import com.rarible.protocol.nft.core.model.ContentMeta
import com.rarible.protocol.nft.core.service.item.meta.CachedContentMetaEntry
import com.rarible.protocol.nft.core.service.item.meta.MediaMetaService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@IntegrationTest
class MediaMetaServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var mediaMetaService: MediaMetaService

    @Autowired
    private lateinit var mongoTemplate: ReactiveMongoTemplate

    @Test
    fun `return content meta from cache`() = runBlocking<Unit> {
        val collection = mongoTemplate.getCollection(CachedContentMetaEntry.CACHE_META_COLLECTION).awaitFirst()
        collection.insertOne(
            Document.parse(
                """
                    {
                      "_id": "https://ipfs.rarible.com/ipfs/Qme8u4pEU25CNB1qP7Ag6W9J9VnvmXNsE7nuqQBn7S3CC8/nft.jpg",
                      "data": {
                        "type": "image/jpeg",
                        "width": 3840,
                        "height": 2160,
                        "size": 5246840,
                        "_class": "com.rarible.protocol.union.enrichment.meta.ContentMeta"
                      },
                      "updateDate": "2021-10-14T14:21:04.528Z",
                      "version": 0,
                      "_class": "com.rarible.core.cache.Cache"
                    }
                """.trimIndent()
            )
        ).awaitFirst()

        collection.insertOne(
            Document.parse(
                """
                    {
                      "_id": "https://ipfs.rarible.com/ipfs/QmUj2wgrN6mYiWfgdbp67fUYwgUxYQcHQnxDWwcBEnZTWK/image.jpeg",
                      "data": {
                        "type": "image/jpeg",
                        "width": 1920,
                        "height": 1080,
                        "_class": "com.rarible.protocol.nft.core.model.MediaMeta"
                      },
                      "updateDate": "2021-06-01T12:55:50.545Z",
                      "version": 0,
                      "_class": "ru.roborox.reactive.cache.Cache"
                    }
                """.trimIndent()
            )
        ).awaitFirst()

        assertThat(
            mediaMetaService.getMediaMetaFromCache("https://ipfs.rarible.com/ipfs/Qme8u4pEU25CNB1qP7Ag6W9J9VnvmXNsE7nuqQBn7S3CC8/nft.jpg", "id")
        ).isEqualTo(
            ContentMeta(
                type = "image/jpeg",
                width = 3840,
                height = 2160,
                size = 5246840
            )
        )

        assertThat(
            mediaMetaService.getMediaMetaFromCache(
                "https://ipfs.rarible.com/ipfs/QmUj2wgrN6mYiWfgdbp67fUYwgUxYQcHQnxDWwcBEnZTWK/image.jpeg",
                "id"
            )
        ).isEqualTo(
            ContentMeta(
                type = "image/jpeg",
                width = 1920,
                height = 1080
            )
        )
    }

    @Test
    fun `return content meta from cache without data`() = runBlocking<Unit> {
        val collection = mongoTemplate.getCollection(CachedContentMetaEntry.CACHE_META_COLLECTION).awaitFirst()
        collection.insertOne(
            Document.parse(
                """
                    {
                      "_id": "https://ipfs.rarible.com/ipfs/Qme8u4pEU25CNB1qP7Ag6W9J9VnvmXNsE7nuqQBn7S3CC8/nft.jpg",
                      "updateDate": "2021-10-14T14:21:04.528Z",
                      "version": 0,
                      "_class": "com.rarible.core.cache.Cache"
                    }
                """.trimIndent()
            )
        ).awaitFirst()

        assertThat(
            mediaMetaService.getMediaMetaFromCache("https://ipfs.rarible.com/ipfs/Qme8u4pEU25CNB1qP7Ag6W9J9VnvmXNsE7nuqQBn7S3CC8/nft.jpg", "id")
        ).isNull()
    }

}
