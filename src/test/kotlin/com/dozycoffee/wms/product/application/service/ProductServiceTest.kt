package com.dozycoffee.wms.product.application.service

import com.dozycoffee.wms.product.application.port.`in`.command.ApplyProductSnapshotCommand
import com.dozycoffee.wms.product.application.port.out.ProductRepository
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.exception.ProductNotFoundException
import com.dozycoffee.wms.product.domain.model.Product
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var productService: ProductService

    private val t0: LocalDateTime = LocalDateTime.of(2026, 10, 13, 10, 0, 0)

    private fun command(
        productStatus: ProductStatus = ProductStatus.ACTIVE,
        occurredAt: LocalDateTime = t0
    ) = ApplyProductSnapshotCommand(7L, "BEAN-001", "예가체프", ProductCategory.BEAN, "KG", 180, productStatus, occurredAt)

    @Nested
    inner class 스냅샷_반영 {

        @Test
        fun `사본이 없으면 스냅샷으로 새로 저장한다`() = runTest {
            whenever(productRepository.findById(7L)).thenReturn(null)
            whenever(productRepository.save(any())).thenAnswer { it.getArgument(0) }

            val applied = productService.apply(command())

            assertThat(applied).isTrue()
            val captor = argumentCaptor<Product>()
            verify(productRepository).save(captor.capture())
            assertThat(captor.firstValue.productId).isEqualTo(7L)
            assertThat(captor.firstValue.productCode).isEqualTo("BEAN-001")
            assertThat(captor.firstValue.sourceOccurredAt).isEqualTo(t0)
        }

        @Test
        fun `더 늦게 발생한 이벤트면 사본을 스냅샷으로 교체한다`() = runTest {
            whenever(productRepository.findById(7L))
                .thenReturn(product().productId(7L).productStatus(ProductStatus.ACTIVE).sourceOccurredAt(t0).build())
            whenever(productRepository.save(any())).thenAnswer { it.getArgument(0) }

            val applied = productService.apply(command(ProductStatus.INACTIVE, t0.plusSeconds(5)))

            assertThat(applied).isTrue()
            val captor = argumentCaptor<Product>()
            verify(productRepository).save(captor.capture())
            assertThat(captor.firstValue.productStatus).isEqualTo(ProductStatus.INACTIVE)
            assertThat(captor.firstValue.sourceOccurredAt).isEqualTo(t0.plusSeconds(5))
        }

        @Test
        fun `같은 이벤트가 다시 오면 저장하지 않는다`() = runTest {
            whenever(productRepository.findById(7L)).thenReturn(product().productId(7L).sourceOccurredAt(t0).build())

            assertThat(productService.apply(command())).isFalse()
            verify(productRepository, never()).save(any())
        }

        @Test
        fun `오래된 이벤트가 늦게 도착해도 최신 사본을 덮지 않는다`() = runTest {
            whenever(productRepository.findById(7L))
                .thenReturn(product().productId(7L).productStatus(ProductStatus.INACTIVE).sourceOccurredAt(t0.plusSeconds(5)).build())

            assertThat(productService.apply(command(ProductStatus.ACTIVE, t0))).isFalse()
            verify(productRepository, never()).save(any())
        }
    }

    @Nested
    inner class 조회 {

        @Test
        fun `상품 ID로 조회한다`() = runTest {
            whenever(productRepository.findById(7L)).thenReturn(product().productId(7L).build())

            assertThat(productService.getById(7L).productId).isEqualTo(7L)
        }

        @Test
        fun `없는 상품을 조회하면 예외를 던진다`() = runTest {
            whenever(productRepository.findById(9L)).thenReturn(null)

            assertThatThrownBy { runBlocking { productService.getById(9L) } }.isInstanceOf(ProductNotFoundException::class.java)
        }

        @Test
        fun `카테고리와 상태로 목록을 조회한다`() = runTest {
            whenever(productRepository.findAll(ProductCategory.BEAN, ProductStatus.ACTIVE)).thenReturn(flowOf(product().build()))

            assertThat(productService.getAll(ProductCategory.BEAN, ProductStatus.ACTIVE).toList()).hasSize(1)
        }
    }
}
