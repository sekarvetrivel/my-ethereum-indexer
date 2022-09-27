package com.rarible.protocol.order.listener.misc

import com.rarible.core.telemetry.metrics.CountingMetric
import com.rarible.ethereum.domain.Blockchain

class OpenSeaOrderDelayLoadMetric(root: String, blockchain: Blockchain) : CountingMetric(
    "$root.opensea.order.delay.load", tag("blockchain", blockchain.value))