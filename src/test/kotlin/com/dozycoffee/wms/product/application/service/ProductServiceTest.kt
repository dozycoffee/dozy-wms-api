package com.dozycoffee.wms.product.application.service

import com.dozycoffee.wms.product.application.port.`in`.command.RegisterProductCommand
import com.dozycoffee.wms.product.application.port.out.ProductRepository
import com.dozycoffee.wms.product.domain.enumeration.ProductCategory
import com.dozycoffee.wms.product.domain.enumeration.ProductStatus
import com.dozycoffee.wms.product.domain.exception.DuplicateProductCodeException
import com.dozycoffee.wms.product.domain.exception.ProductNotFoundException
import com.dozycoffee.wms.product.domain.model.Product
import com.dozycoffee.wms.product.fixture.ProductTestBuilder.Companion.product
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class ProductServiceTest {

    @Mock
    private lateinit var productRepository: ProductRepository

    @InjectMocks
    private lateinit var productService: ProductService

    @Nested
    inner class 상품_등록 {

        @Test
        fun `정상적인 정보로 등록하면 저장된 상품 정보를 반환한다`() = runTest {
            val command = RegisterProductCommand(
                "PRD-0001", "콜롬비아 원두", ProductCategory.BEAN, "KG", 365
            )
            val saved: Product = product().productId(1L).build()
            whenever(productRepository.existsByProductCode("PRD-0001")).thenReturn(false)
            whenever(productRepository.save(any())).thenReturn(saved)

            val result = productService.register(command)

            assertThat(result.productId).isEqualTo(1L)
            assertThat(result.productStatus).isEqualTo(ProductStatus.ACTIVE)

            val captor = argumentCaptor<Product>()
            verify(productRepository).save(captor.capture())
            assertThat(captor.firstValue.productStatus).isEqualTo(ProductStatus.ACTIVE)
        }

        @Test
        fun `이미 등록된 상품 코드면 예외를 던진다`() = runTest {
            val command = RegisterProductCommand(
                "PRD-0001", "콜롬비아 원두", ProductCategory.BEAN, "KG", 365
            )
            whenever(productRepository.existsByProductCode("PRD-0001")).thenReturn(true)

            assertThatThrownBy { runBlocking { productService.register(command) } }
                .isInstanceOf(DuplicateProductCodeException::class.java)
        }
    }

    @Nested
    inner class 상품_활성화 {

        @Test
        fun `존재하는 상품을 활성화하면 상태가 ACTIVE로 바뀐다`() = runTest {
            val inactive: Product = product().productId(1L).productStatus(ProductStatus.INACTIVE).build()
            whenever(productRepository.findById(1L)).thenReturn(inactive)
            whenever(productRepository.save(any())).thenAnswer { invocation -> invocation.getArgument(0) }

            val result = productService.activate(1L)

            assertThat(result.productStatus).isEqualTo(ProductStatus.ACTIVE)
        }

        @Test
        fun `존재하지 않는 상품을 활성화하면 예외를 던진다`() = runTest {
            whenever(productRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { productService.activate(1L) } }
                .isInstanceOf(ProductNotFoundException::class.java)
        }
    }

    @Nested
    inner class 상품_비활성화 {

        @Test
        fun `존재하는 상품을 비활성화하면 상태가 INACTIVE로 바뀐다`() = runTest {
            val active: Product = product().productId(1L).productStatus(ProductStatus.ACTIVE).build()
            whenever(productRepository.findById(1L)).thenReturn(active)
            whenever(productRepository.save(any())).thenAnswer { invocation -> invocation.getArgument(0) }

            val result = productService.deactivate(1L)

            assertThat(result.productStatus).isEqualTo(ProductStatus.INACTIVE)
        }

        @Test
        fun `존재하지 않는 상품을 비활성화하면 예외를 던진다`() = runTest {
            whenever(productRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { productService.deactivate(1L) } }
                .isInstanceOf(ProductNotFoundException::class.java)
        }
    }

    @Nested
    inner class 상품_단건_조회 {

        @Test
        fun `존재하는 상품을 조회하면 결과를 반환한다`() = runTest {
            val found: Product = product().productId(1L).build()
            whenever(productRepository.findById(1L)).thenReturn(found)

            val result = productService.getById(1L)

            assertThat(result.productId).isEqualTo(1L)
        }

        @Test
        fun `존재하지 않는 상품을 조회하면 예외를 던진다`() = runTest {
            whenever(productRepository.findById(1L)).thenReturn(null)

            assertThatThrownBy { runBlocking { productService.getById(1L) } }
                .isInstanceOf(ProductNotFoundException::class.java)
        }
    }
}
