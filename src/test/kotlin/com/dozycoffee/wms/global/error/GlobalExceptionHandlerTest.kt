package com.dozycoffee.wms.global.error

import com.dozycoffee.auth.test.WithDozyPrincipal
import com.dozycoffee.wms.product.domain.exception.DuplicateProductCodeException
import com.dozycoffee.wms.product.domain.exception.ProductErrorCode
import com.dozycoffee.wms.product.domain.exception.ProductNotFoundException
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@WithDozyPrincipal(roles = ["wms:warehouse_admin"])
@WebFluxTest(controllers = [GlobalExceptionHandlerTest.FixtureController::class])
@Import(GlobalExceptionHandlerTest.FixtureController::class)
class GlobalExceptionHandlerTest {

    @RestController
    class FixtureController {

        @GetMapping("/fixture/not-found")
        fun notFound(): String = throw ProductNotFoundException()

        @GetMapping("/fixture/conflict")
        fun conflict(): String = throw DuplicateProductCodeException()

        @GetMapping("/fixture/invalid-domain-value")
        fun invalidDomainValue(): String = throw InvalidDomainValueException(ProductErrorCode.INVALID_PRODUCT_CODE)

        @GetMapping("/fixture/unexpected")
        fun unexpected(): String = throw IllegalStateException("jdbc:mysql://internal-host/secret_table")

        @PostMapping("/fixture/body")
        fun body(@Validated @RequestBody request: SampleRequest): String = request.name

        @GetMapping("/fixture/param")
        fun param(@RequestParam @Min(1) size: Int): String = size.toString()
    }

    data class SampleRequest(
        @field:NotBlank(message = "이름은 필수입니다.")
        val name: String
    )

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @Test
    fun `BusinessException은 ErrorCode의 code와 상태로 Problem Details를 반환한다`() {
        webTestClient.get().uri("/fixture/not-found")
            .header("X-Trace-Id", "trace-0001")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectHeader().valueEquals("X-Trace-Id", "trace-0001")
            .expectBody()
            .jsonPath("$.type").isEqualTo("https://docs.dozycoffee.com/errors/product-not-found")
            .jsonPath("$.title").isEqualTo("Product not found")
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.detail").isEqualTo("존재하지 않는 상품입니다.")
            .jsonPath("$.instance").isEqualTo("/fixture/not-found")
            .jsonPath("$.code").isEqualTo("PRODUCT_NOT_FOUND")
            .jsonPath("$.traceId").isEqualTo("trace-0001")
    }

    @Test
    fun `ErrorType CONFLICT는 409로 매핑한다`() {
        webTestClient.get().uri("/fixture/conflict")
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody().jsonPath("$.code").isEqualTo("PRODUCT_DUPLICATE_PRODUCT_CODE")
    }

    @Test
    fun `도메인 값 검증 예외는 400과 해당 ErrorCode를 반환한다`() {
        webTestClient.get().uri("/fixture/invalid-domain-value")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("$.code").isEqualTo("PRODUCT_INVALID_PRODUCT_CODE")
    }

    @Test
    fun `처리하지 못한 예외는 내부 정보 없이 500 INTERNAL_ERROR를 반환한다`() {
        val body: String = String(
            requireNotNull(
                webTestClient.get().uri("/fixture/unexpected")
                    .exchange()
                    .expectStatus().isEqualTo(500)
                    .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                    .expectBody()
                    .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
                    .jsonPath("$.detail").isEqualTo("서버 내부 오류가 발생했습니다.")
                    .returnResult().responseBody
            )
        )

        org.assertj.core.api.Assertions.assertThat(body)
            .doesNotContain("secret_table", "IllegalStateException", "jdbc:mysql")
    }

    @Test
    fun `요청 본문 검증 실패는 400 VALIDATION_FAILED와 errors 목록을 반환한다`() {
        webTestClient.post().uri("/fixture/body")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("""{"name": ""}""")
            .exchange()
            .expectStatus().isBadRequest
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
            .jsonPath("$.detail").isEqualTo("요청 값이 올바르지 않습니다.")
            .jsonPath("$.errors[0].field").isEqualTo("name")
            .jsonPath("$.errors[0].code").isEqualTo("NotBlank")
            .jsonPath("$.errors[0].message").isEqualTo("이름은 필수입니다.")
    }

    @Test
    fun `파라미터 제약 위반은 400 VALIDATION_FAILED와 errors 목록을 반환한다`() {
        webTestClient.get().uri("/fixture/param?size=0")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
            .jsonPath("$.errors[0].field").isEqualTo("size")
            .jsonPath("$.errors[0].code").isEqualTo("Min")
    }

    @Test
    fun `깨진 JSON 본문은 500이 아니라 400 VALIDATION_FAILED다`() {
        webTestClient.post().uri("/fixture/body")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{not-json")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody().jsonPath("$.code").isEqualTo("VALIDATION_FAILED")
    }

    @Test
    fun `허용하지 않는 HTTP 메서드는 405 METHOD_NOT_ALLOWED다`() {
        webTestClient.post().uri("/fixture/not-found")
            .exchange()
            .expectStatus().isEqualTo(405)
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody().jsonPath("$.code").isEqualTo("METHOD_NOT_ALLOWED")
    }

    @Test
    fun `지원하지 않는 Content-Type은 415 UNSUPPORTED_MEDIA_TYPE이다`() {
        webTestClient.post().uri("/fixture/body")
            .contentType(MediaType.TEXT_PLAIN)
            .bodyValue("name")
            .exchange()
            .expectStatus().isEqualTo(415)
            .expectBody().jsonPath("$.code").isEqualTo("UNSUPPORTED_MEDIA_TYPE")
    }

    @Test
    fun `매핑되지 않은 경로는 404 NOT_FOUND다`() {
        webTestClient.get().uri("/fixture/no-such-path")
            .exchange()
            .expectStatus().isNotFound
            .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("NOT_FOUND")
            .jsonPath("$.traceId").isNotEmpty
    }

    @Test
    fun `요청에 X-Trace-Id가 없으면 생성한 값이 응답 헤더와 본문에 같이 실린다`() {
        val result = webTestClient.get().uri("/fixture/not-found").exchange().expectBody().returnResult()

        val header: String? = result.responseHeaders.getFirst("X-Trace-Id")
        org.assertj.core.api.Assertions.assertThat(header).matches("[0-9a-f]{32}")
        org.assertj.core.api.Assertions.assertThat(String(requireNotNull(result.responseBody)))
            .contains("\"traceId\":\"$header\"")
    }
}
