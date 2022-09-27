package com.rarible.protocol.nft.api.converter

import com.rarible.ethereum.domain.EthUInt256
import com.rarible.protocol.dto.ActivityContinuationDto
import com.rarible.protocol.dto.NftActivityFilterAllDto
import com.rarible.protocol.dto.NftActivityFilterByCollectionDto
import com.rarible.protocol.dto.NftActivityFilterByItemAndOwnerDto
import com.rarible.protocol.dto.NftActivityFilterByItemDto
import com.rarible.protocol.dto.NftActivityFilterByUserDto
import com.rarible.protocol.dto.NftActivityFilterDto
import com.rarible.protocol.nft.api.configuration.NftIndexerApiProperties
import com.rarible.protocol.nft.core.repository.history.ActivityItemHistoryFilter
import com.rarible.protocol.nft.core.repository.history.ActivitySort
import com.rarible.protocol.nft.core.repository.history.CollectionActivityItemHistoryFilter
import com.rarible.protocol.nft.core.repository.history.ItemActivityItemHistoryFilter
import com.rarible.protocol.nft.core.repository.history.ItemAndOwnerActivityItemHistoryFilter
import com.rarible.protocol.nft.core.repository.history.UserActivityItemHistoryFilter
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class ActivityHistoryFilterConverter(properties: NftIndexerApiProperties) {
    private val skipHeavyRequest = properties.skipHeavyRequest

    fun convert(sort: ActivitySort, source: NftActivityFilterDto, activityContinuation: ActivityContinuationDto?): List<ActivityItemHistoryFilter> {
        val continuation = activityContinuation?.let { ContinuationConverter.convert(it) }

        return when (source) {
            is NftActivityFilterAllDto -> source.types.map {
                when (it) {
                    NftActivityFilterAllDto.Types.TRANSFER -> ActivityItemHistoryFilter.AllTransfer(sort, continuation)
                    NftActivityFilterAllDto.Types.MINT -> ActivityItemHistoryFilter.AllMint(sort, continuation)
                    NftActivityFilterAllDto.Types.BURN -> ActivityItemHistoryFilter.AllBurn(sort, continuation)
                }
            }
            is NftActivityFilterByUserDto -> source.types.map {
                val users = if (source.users.size > 1 && skipHeavyRequest) listOf(source.users.first()) else source.users
                val from = source.from?.let { from -> Instant.ofEpochSecond(from) }
                val to = source.to?.let { to -> Instant.ofEpochSecond(to) }
                when (it) {
                    NftActivityFilterByUserDto.Types.TRANSFER_FROM -> UserActivityItemHistoryFilter.ByUserTransferFrom(
                        sort,
                        users,
                        from,
                        to,
                        continuation
                    )
                    NftActivityFilterByUserDto.Types.TRANSFER_TO -> UserActivityItemHistoryFilter.ByUserTransferTo(
                        sort,
                        users,
                        from,
                        to,
                        continuation
                    )
                    NftActivityFilterByUserDto.Types.MINT -> UserActivityItemHistoryFilter.ByUserMint(
                        sort,
                        users,
                        from,
                        to,
                        continuation
                    )
                    NftActivityFilterByUserDto.Types.BURN -> UserActivityItemHistoryFilter.ByUserBurn(
                        sort,
                        users,
                        from,
                        to,
                        continuation
                    )
                }
            }
            is NftActivityFilterByItemDto -> source.types.map {
                val contract = source.contract
                val tokenId = EthUInt256.of(source.tokenId)
                when (it) {
                    NftActivityFilterByItemDto.Types.TRANSFER -> ItemActivityItemHistoryFilter.ByItemTransfer(
                        sort,
                        contract,
                        tokenId,
                        continuation
                    )
                    NftActivityFilterByItemDto.Types.MINT -> ItemActivityItemHistoryFilter.ByItemMint(
                        sort,
                        contract,
                        tokenId,
                        continuation
                    )
                    NftActivityFilterByItemDto.Types.BURN -> ItemActivityItemHistoryFilter.ByItemBurn(
                        sort,
                        contract,
                        tokenId,
                        continuation
                    )
                }
            }
            is NftActivityFilterByItemAndOwnerDto -> source.types.map {
                val contract = source.contract
                val tokenId = EthUInt256.of(source.tokenId)
                val owner = source.owner
                when (it) {
                    NftActivityFilterByItemAndOwnerDto.Types.TRANSFER -> ItemAndOwnerActivityItemHistoryFilter.ByItemAndOwnerTransfer(
                        sort,
                        contract,
                        tokenId,
                        owner,
                        continuation
                    )
                    NftActivityFilterByItemAndOwnerDto.Types.MINT -> ItemAndOwnerActivityItemHistoryFilter.ByItemAndOwnerMint(
                        sort,
                        contract,
                        tokenId,
                        owner,
                        continuation
                    )
                }
            }
            is NftActivityFilterByCollectionDto -> source.types.map {
                val contract = source.contract
                when (it) {
                    NftActivityFilterByCollectionDto.Types.TRANSFER -> CollectionActivityItemHistoryFilter.ByCollectionTransfer(
                        sort,
                        contract,
                        continuation
                    )
                    NftActivityFilterByCollectionDto.Types.MINT -> CollectionActivityItemHistoryFilter.ByCollectionMint(
                        sort,
                        contract,
                        continuation
                    )
                    NftActivityFilterByCollectionDto.Types.BURN -> CollectionActivityItemHistoryFilter.ByCollectionBurn(
                        sort,
                        contract,
                        continuation
                    )
                }
            }
        }
    }
}
