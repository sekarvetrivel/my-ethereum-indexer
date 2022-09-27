package com.rarible.protocol.nft.core.configuration

import com.rarible.core.mongo.configuration.EnableRaribleMongo
import com.rarible.ethereum.converters.EnableScaletherMongoConversions
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.mongodb.config.EnableMongoAuditing
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@EnableMongoAuditing
@EnableScaletherMongoConversions
@EnableRaribleMongo
@EnableReactiveMongoRepositories(basePackageClasses = [com.rarible.protocol.nft.core.repository.Package::class])
@ComponentScan(basePackageClasses = [com.rarible.protocol.nft.core.repository.Package::class])
class RepositoryConfiguration
