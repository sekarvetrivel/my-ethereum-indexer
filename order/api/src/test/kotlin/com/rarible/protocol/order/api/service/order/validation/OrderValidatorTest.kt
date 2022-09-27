package com.rarible.protocol.order.api.service.order.validation

import com.rarible.core.common.nowMillis
import com.rarible.ethereum.common.NewKeys
import com.rarible.ethereum.domain.EthUInt256
import com.rarible.ethereum.sign.domain.EIP712Domain
import com.rarible.protocol.dto.OrderDto
import com.rarible.protocol.dto.OrderFormDto
import com.rarible.protocol.dto.RaribleV2OrderDto
import com.rarible.protocol.dto.parser.AddressParser
import com.rarible.protocol.order.api.controller.OrderControllerFt
import com.rarible.protocol.order.api.data.createOrderVersion
import com.rarible.protocol.order.api.data.toForm
import com.rarible.protocol.order.api.exceptions.OrderUpdateException
import com.rarible.protocol.order.api.integration.IntegrationTest
import com.rarible.protocol.order.api.service.order.OrderService
import com.rarible.protocol.order.core.data.createOrderRaribleV2DataV1
import com.rarible.protocol.order.core.model.*
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import org.apache.commons.lang3.RandomUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.utils.Numeric
import scalether.domain.Address
import scalether.domain.AddressFactory
import java.math.BigInteger

@IntegrationTest
@Import(OrderControllerFt.TestOrderServiceConfiguration::class)
class OrderValidatorTest {


    @Autowired
    lateinit var orderService: OrderService

    private val validator = OrderValidator(mockk(), mockk())

    private val make = Asset(Erc721AssetType(token = AddressParser.parse("0xa7e550df5593049dcf7f8088d0272c7d8f353cd2"),
    tokenId = EthUInt256(BigInteger("5200435271198095841614392404294920383202070526176298022107874038349490552835"))
    ), EthUInt256.ONE)
    private val take = Asset(
        type = EthAssetType,
        value = EthUInt256(BigInteger("1"))
    )//Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.of(5))

    @AfterEach
    fun afterEach() {
        clearMocks(orderService)
    }

    @Test
    fun `make change validation`() {
        val order = createOrderVersion(make, take)
        coEvery {
            testCreateOrderUsingPutRequest(order.copy(make = order.make.copy(value = EthUInt256.of(11))))}
        assertThrows<OrderUpdateException> {
            validator.validate(
                order.toOrderExactFields(),
                order.copy(make = order.make.copy(value = EthUInt256.of(9)))
            )
        }
        validator.validate(
            order.toOrderExactFields(),
            order.copy(make = order.make.copy(value = EthUInt256.of(11)))
        )
    }

    private suspend fun testCreateOrderUsingPutRequest(orderVersion: OrderVersion){
        validator.validateOrderVersion(orderVersion)
    }

        @Test
    suspend fun `price validation`() {
        val order = createOrderVersion(make, take)
        assertThrows<OrderUpdateException> {
            validator.validate(
                order.toOrderExactFields(), order.copy(
                    make = order.make.copy(value = EthUInt256.of(20)),
                    take = order.make.copy(value = EthUInt256.of(11))
                )
            )
        }
        val createOrder = createOrder()//createOrder(signer, Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN), salt, data)
        val (privateKey, _, signer) = generateNewKeys()

        val formDto = createOrder.toForm(EIP712Domain("", "", BigInteger.ONE, AddressFactory.create()), privateKey)

        validator.validateOrderVersion(orderService.convertFormToVersion(formDto))
        validator.validate(
            order.toOrderExactFields(), order.copy(
                make = order.make.copy(value = EthUInt256.of(20)),
                take = order.make.copy(value = EthUInt256.of(10))
            )
        )
        validator.validate(
            order.toOrderExactFields(), order.copy(
                make = order.make.copy(value = EthUInt256.of(20)),
                take = order.make.copy(value = EthUInt256.of(9))
            )
        )
    }


    private suspend fun testCreateOrderUsingPutRequest(
        salt: EthUInt256,
        data: OrderData = createOrderRaribleV2DataV1(),
        upsert: suspend (OrderFormDto) -> OrderDto
    ) {
        val (privateKey, _, signer) = generateNewKeys()
        val order = createOrder()//createOrder(signer, Asset(Erc20AssetType(AddressFactory.create()), EthUInt256.TEN), salt, data)

        coEvery { orderService.put(any()) } returns order

        val formDto = order.toForm(EIP712Domain("", "", BigInteger.ONE, AddressFactory.create()), privateKey)

        val orderDto = upsert(formDto)
        Assertions.assertThat((orderDto as RaribleV2OrderDto).make.value).isEqualTo(BigInteger.TEN)
        //println(objectMapper.writeValueAsString(orderDto))
    }

    protected fun generateNewKeys(privateKey0: BigInteger? = null): NewKeys {
        val privateKey = privateKey0 ?: Numeric.toBigInt(RandomUtils.nextBytes(32))
        val publicKey = Sign.publicKeyFromPrivate(privateKey)
        val signer = Address.apply(Keys.getAddressFromPrivateKey(privateKey))
        return NewKeys(privateKey, publicKey, signer)
    }

    fun createOrder() =
        Order(
            maker = AddressParser.parse("0x0b7f5790f119aa05b581c9e315437cf500449645"), //AddressFactory.create(),
            taker = null,
            make = createMake("1"),
            take = createEth(),
            makeStock = EthUInt256(BigInteger("1")),
            type = OrderType.RARIBLE_V2,
            fill = EthUInt256.ZERO,
            cancelled = false,
            salt = EthUInt256(BigInteger("57575976112221712031749901136025211353311563194191846159454969863334581796983")),
            start = null,
            end = null,
            data = OrderRaribleV2DataV1(emptyList(), emptyList()),
            signature = null,
            createdAt = nowMillis(),
            lastUpdateAt = nowMillis()
        )

    fun createEth(): Asset {
        return Asset(
            type = EthAssetType,
            value = EthUInt256(BigInteger("1"))
        )
    }
    fun createMake(value:String): Asset {
        return Asset(
            type = createAssetType("0xa7e550df5593049dcf7f8088d0272c7d8f353cd2"),
            value = EthUInt256(BigInteger(value))
        )
    }

    fun createAssetType(address:String): AssetType {
        return Erc721AssetType(token = AddressParser.parse(address),
            tokenId = EthUInt256(BigInteger("5200435271198095841614392404294920383202070526176298022107874038349490552835")))
    }



    @Test
    fun `bid price validation`() {
        val order = createOrderVersion(make, take)
        assertThrows<OrderUpdateException> {
            validator.validate(
                order.toOrderExactFields(), order.copy(
                    make = order.make.copy(value = EthUInt256.of(9)),
                    take = order.make.copy(value = EthUInt256.of(5))
                )
            )
        }
        validator.validate(
            order.toOrderExactFields(), order.copy(
                make = order.make.copy(value = EthUInt256.of(11)),
                take = order.make.copy(value = EthUInt256.of(5))
            )
        )
    }
}
