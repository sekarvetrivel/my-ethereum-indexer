package com.rarible.protocol.order.core.service

import com.rarible.protocol.order.core.model.Transfer
import io.daonomic.rpc.domain.Binary
import io.daonomic.rpc.domain.Word
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import scalether.domain.Address
import java.math.BigInteger

internal class CallDataEncoderTest {
    private val callDataEncoder = CallDataEncoder()

    @Test
    fun `should encode sell ERC721 callData`() {
        val transfer = Transfer.Erc721Transfer(
            from = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            to = Address.ZERO(),
            tokenId = BigInteger.valueOf(110711),
            safe = false
        )
        val transferCallData = callDataEncoder.encodeTransferCallData(transfer)
        assertThat(transferCallData.callData).isEqualTo(Binary.apply("0x23b872dd00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001b077"))
        assertThat(transferCallData.replacementPattern).isEqualTo(Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `should decode sell ERC721 callData`() {
        val transfer = Transfer.Erc721Transfer(
            from = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            to = Address.ZERO(),
            tokenId = BigInteger.valueOf(110711),
            safe = false
        )
        val decodedTransfer = callDataEncoder.decodeTransfer(Binary.apply("0x23b872dd00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001b077"))
        assertThat(decodedTransfer).isEqualTo(transfer)
    }

    @Test
    fun `should encode buy ERC721 callData`() {
        val transfer = Transfer.Erc721Transfer(
            from = Address.ZERO(),
            to = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            tokenId = BigInteger.valueOf(110711),
            safe = false
        )
        val transferCallData = callDataEncoder.encodeTransferCallData(transfer)
        assertThat(transferCallData.callData).isEqualTo(Binary.apply("0x23b872dd000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000001b077"))
        assertThat(transferCallData.replacementPattern).isEqualTo(Binary.apply("0x00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `should decode buy ERC721 callData`() {
        val transfer = Transfer.Erc721Transfer(
            from = Address.ZERO(),
            to = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            tokenId = BigInteger.valueOf(110711),
            safe = false
        )
        val decodedTransfer = callDataEncoder.decodeTransfer(Binary.apply("0x23b872dd000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000001b077"))
        assertThat(decodedTransfer).isEqualTo(transfer)
    }

    @Test
    fun `should encode sell ERC1155 callData`() {
        val transfer = Transfer.Erc1155Transfer(
            from = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            to = Address.ZERO(),
            tokenId = BigInteger.valueOf(110249),
            value = BigInteger.valueOf(3),
            data = Binary.apply(ByteArray(0))
        )
        val transferCallData = callDataEncoder.encodeTransferCallData(transfer)
        assertThat(transferCallData.callData).isEqualTo(Binary.apply("0xf242432a00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001aea9000000000000000000000000000000000000000000000000000000000000000300000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(transferCallData.replacementPattern).isEqualTo(Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `should decode sell ERC1155 callData`() {
        val transfer = Transfer.Erc1155Transfer(
            from = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            to = Address.ZERO(),
            tokenId = BigInteger.valueOf(110249),
            value = BigInteger.valueOf(3),
            data = Binary.apply(ByteArray(0))
        )
        val decodedTransfer = callDataEncoder.decodeTransfer(Binary.apply("0xf242432a00000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b60000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000001aea9000000000000000000000000000000000000000000000000000000000000000300000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(decodedTransfer).isEqualTo(transfer)
    }

    @Test
    fun `should encode buy ERC1155 callData`() {
        val transfer = Transfer.Erc1155Transfer(
            from = Address.ZERO(),
            to = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            tokenId = BigInteger.valueOf(110249),
            value = BigInteger.valueOf(2),
            data = Binary.apply(ByteArray(0))
        )
        val transferCallData = callDataEncoder.encodeTransferCallData(transfer)
        assertThat(transferCallData.callData).isEqualTo(Binary.apply("0xf242432a000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000001aea9000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(transferCallData.replacementPattern).isEqualTo(Binary.apply("0x00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `should decode buy ERC1155 callData`() {
        val transfer = Transfer.Erc1155Transfer(
            from = Address.ZERO(),
            to = Address.apply("0x47921676a46ccfe3d80b161c7b4ddc8ed9e716b6"),
            tokenId = BigInteger.valueOf(110249),
            value = BigInteger.valueOf(2),
            data = Binary.apply(ByteArray(0))
        )
        val decodedTransfer = callDataEncoder.decodeTransfer(Binary.apply("0xf242432a000000000000000000000000000000000000000000000000000000000000000000000000000000000000000047921676a46ccfe3d80b161c7b4ddc8ed9e716b6000000000000000000000000000000000000000000000000000000000001aea9000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000a00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(decodedTransfer).isEqualTo(transfer)
    }

    @Test
    fun `should decode sell ERC721 delegate callData`() {
        val transfer = Transfer.MerkleValidatorErc721Transfer(
            from = Address.apply("0x7dff47820fa7ef1c2eae54803b9c06a6fcace40d"),
            to = Address.ZERO(),
            token = Address.apply("0x4bb33f6e69fd62cf3abbcc6f1f43b94a5d572c2b"),
            tokenId = BigInteger.valueOf(811),
            root = Word.apply(ByteArray(32)),
            proof = emptyList(),
            safe = false
        )
        val decodedTransfer = callDataEncoder.decodeTransfer(Binary.apply("0xfb16a5950000000000000000000000007dff47820fa7ef1c2eae54803b9c06a6fcace40d00000000000000000000000000000000000000000000000000000000000000000000000000000000000000004bb33f6e69fd62cf3abbcc6f1f43b94a5d572c2b000000000000000000000000000000000000000000000000000000000000032b000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(decodedTransfer).isEqualTo(transfer)
    }

    @Test
    fun `should encode sell ERC721 delegate callData`() {
        val transfer = Transfer.MerkleValidatorErc721Transfer(
            from = Address.apply("0x552544565b93cca46e8a9d11f4a71a78c6d06f36"),
            to = Address.ZERO(),
            token = Address.apply("0x03e99afd576eaf6b818d58a4efc59d2d0cbf1678"),
            tokenId = BigInteger.valueOf(2765),
            root = Word.apply(ByteArray(32)),
            proof = emptyList(),
            safe = false
        )
        val transferCallData = callDataEncoder.encodeTransferCallData(transfer)
        assertThat(transferCallData.callData).isEqualTo(Binary.apply("0xfb16a595000000000000000000000000552544565b93cca46e8a9d11f4a71a78c6d06f36000000000000000000000000000000000000000000000000000000000000000000000000000000000000000003e99afd576eaf6b818d58a4efc59d2d0cbf16780000000000000000000000000000000000000000000000000000000000000acd000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(transferCallData.replacementPattern).isEqualTo(Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `should decode sell ERC1155 delegate callData`() {
        val transfer = Transfer.MerkleValidatorErc1155Transfer(
            from = Address.apply("0x77a8703db1aeee04405f8a37fde59f669adfa429"),
            to = Address.ZERO(),
            token = Address.apply("0x495f947276749ce646f68ac8c248420045cb7b5e"),
            tokenId = BigInteger("57920695457072532059112617018159441295166854042840448471990455457112129536001"),
            value = BigInteger.valueOf(1),
            root = Word.apply(ByteArray(32)),
            proof = emptyList(),
        )
        val decodedTransfer = callDataEncoder.decodeTransfer(Binary.apply("0x96809f9000000000000000000000000077a8703db1aeee04405f8a37fde59f669adfa4290000000000000000000000000000000000000000000000000000000000000000000000000000000000000000495f947276749ce646f68ac8c248420045cb7b5e800df3ae6a276f20679451f55b1b8a60368e4e720000000000087700000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(decodedTransfer).isEqualTo(transfer)
    }

    @Test
    fun `should encode sell ERC1155 delegate callData`() {
        val transfer = Transfer.MerkleValidatorErc1155Transfer(
            from = Address.apply("0x77a8703db1aeee04405f8a37fde59f669adfa429"),
            to = Address.ZERO(),
            token = Address.apply("0x495f947276749ce646f68ac8c248420045cb7b5e"),
            tokenId = BigInteger("57920695457072532059112617018159441295166854042840448471990455457112129536001"),
            value = BigInteger.valueOf(1),
            root = Word.apply(ByteArray(32)),
            proof = emptyList(),
        )
        val transferCallData = callDataEncoder.encodeTransferCallData(transfer)
        assertThat(transferCallData.callData).isEqualTo(Binary.apply("0x96809f9000000000000000000000000077a8703db1aeee04405f8a37fde59f669adfa4290000000000000000000000000000000000000000000000000000000000000000000000000000000000000000495f947276749ce646f68ac8c248420045cb7b5e800df3ae6a276f20679451f55b1b8a60368e4e720000000000087700000000010000000000000000000000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000e00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(transferCallData.replacementPattern).isEqualTo(Binary.apply("0x000000000000000000000000000000000000000000000000000000000000000000000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `should decode buy ERC721 delegate callData`() {
        val transfer = Transfer.MerkleValidatorErc721Transfer(
            from = Address.ZERO(),
            to = Address.apply("0x60575a92ac6f5d77e6dd9d73656b411d7a5e58bb"),
            token = Address.apply("0x2acab3dea77832c09420663b0e1cb386031ba17b"),
            tokenId = BigInteger.valueOf(1345),
            root = Word.apply(ByteArray(32)),
            proof = emptyList(),
            safe = false
        )
        val transferCallData = callDataEncoder.encodeTransferCallData(transfer)
        assertThat(transferCallData.callData).isEqualTo(Binary.apply("0xfb16a595000000000000000000000000000000000000000000000000000000000000000000000000000000000000000060575a92ac6f5d77e6dd9d73656b411d7a5e58bb0000000000000000000000002acab3dea77832c09420663b0e1cb386031ba17b0000000000000000000000000000000000000000000000000000000000000541000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(transferCallData.replacementPattern).isEqualTo(Binary.apply("0x00000000ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"))
    }

    @Test
    fun `should encode buy ERC721 delegate callData`() {
        val transfer = Transfer.MerkleValidatorErc721Transfer(
            from = Address.ZERO(),
            to = Address.apply("0x60575a92ac6f5d77e6dd9d73656b411d7a5e58bb"),
            token = Address.apply("0x2acab3dea77832c09420663b0e1cb386031ba17b"),
            tokenId = BigInteger.valueOf(1345),
            root = Word.apply(ByteArray(32)),
            proof = emptyList(),
            safe = false
        )
        val decodedTransfer = callDataEncoder.decodeTransfer(Binary.apply("0xfb16a595000000000000000000000000000000000000000000000000000000000000000000000000000000000000000060575a92ac6f5d77e6dd9d73656b411d7a5e58bb0000000000000000000000002acab3dea77832c09420663b0e1cb386031ba17b0000000000000000000000000000000000000000000000000000000000000541000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000c00000000000000000000000000000000000000000000000000000000000000000"))
        assertThat(decodedTransfer).isEqualTo(transfer)
    }
}