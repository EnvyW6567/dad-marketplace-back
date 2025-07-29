package org.envyw.dadmarketplace.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings({"rawtypes", "unchecked"})
@ExtendWith(MockitoExtension.class)
@DisplayName("ExternalApiService 테스트")
class ExternalApiServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private ExternalApiService externalApiService;

    private final String EXTERNAL_API_BASE_URL = "https://api.darkerdb.com/v1";

    @BeforeEach
    void setUp(TestInfo testInfo) {
        ReflectionTestUtils.setField(externalApiService,
                "EXTERNAL_API_BASE_URL", "https://api.darkerdb.com/v1");

        if (testInfo.getDisplayName().contains("skip setup")) {
            return;
        }

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    @DisplayName("정상적인 외부 API 요청이 성공해야 한다")
    void shouldSuccessfullyProxyExternalApiRequest() {
        // Given
        String path = "items/search";
        String queryParams = "name=sword&rarity=epic";
        Map<String, Object> mockResponse = Map.of("items", "test data");
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(mockResponse);

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, queryParams);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isEqualTo(mockResponse);
                })
                .verifyComplete();

        verify(requestHeadersUriSpec).uri(EXTERNAL_API_BASE_URL + "/items/search?name=sword&rarity=epic");
    }

    @Test
    @DisplayName("쿼리 파라미터 없이 외부 API 요청이 성공해야 한다")
    void shouldSuccessfullyProxyRequestWithoutQueryParams() {
        // Given
        String path = "/items";
        Map<String, Object> mockResponse = Map.of("data", "all items");
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(mockResponse);

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    assertThat(response.getBody()).isEqualTo(mockResponse);
                })
                .verifyComplete();

        verify(requestHeadersUriSpec).uri(EXTERNAL_API_BASE_URL + path);
    }

    @Test
    @DisplayName("빈 경로로 요청 시 BAD_REQUEST를 반환해야 한다 - skip setup")
    void shouldReturnBadRequestForEmptyPath() {
        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest("", "query=test");

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isInstanceOf(Map.class);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertThat(body.get("error")).isEqualTo("Path cannot be empty");
                })
                .verifyComplete();

        verify(webClient, never()).get();
    }

    @Test
    @DisplayName("null 경로로 요청 시 BAD_REQUEST를 반환해야 한다 - skip setup")
    void shouldReturnBadRequestForNullPath() {
        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(null, "query=test");

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(response.getBody()).isInstanceOf(Map.class);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertThat(body.get("error")).isEqualTo("Path cannot be empty");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("슬래시로 시작하는 경로를 올바르게 처리해야 한다")
    void shouldHandlePathStartingWithSlash() {
        // Given
        String path = "/items/weapons";
        Map<String, Object> mockResponse = Map.of("weapons", "data");
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(mockResponse);

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, null);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(requestHeadersUriSpec).uri(EXTERNAL_API_BASE_URL + "/items/weapons");
    }

    @Test
    @DisplayName("WebClientResponseException 404 오류를 적절히 처리해야 한다")
    void shouldHandleWebClientResponseException404() {
        // Given
        String path = "items/nonexistent";
        WebClientResponseException exception = WebClientResponseException.create(
                404, "Not Found", null, null, null);

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.error(exception));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(response.getBody()).isInstanceOf(Map.class);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertThat(body.get("error")).asString().contains("External API client error");
                    assertThat(body.get("originalStatus")).isEqualTo(404);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("WebClientResponseException 500 오류를 BAD_GATEWAY로 처리해야 한다")
    void shouldHandleWebClientResponseException500() {
        // Given
        String path = "items/error";
        WebClientResponseException exception = WebClientResponseException.create(
                500, "Internal Server Error", null, null, null);

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.error(exception));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
                    assertThat(response.getBody()).isInstanceOf(Map.class);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertThat(body.get("error")).asString().contains("External API server error");
                    assertThat(body.get("originalStatus")).isEqualTo(500);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("WebClientException 연결 오류를 SERVICE_UNAVAILABLE로 처리해야 한다")
    void shouldHandleWebClientException() {
        // Given
        String path = "items/connection-error";
        WebClientException exception = mock(WebClientException.class);

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.error(exception));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(response.getBody()).isInstanceOf(Map.class);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertThat(body.get("error")).asString().contains("External API connection error");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("타임아웃 오류를 적절히 처리해야 한다")
    void shouldHandleTimeoutException() {
        // Given
        String path = "items/timeout";
        TimeoutException exception = new TimeoutException("Request timeout");

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.error(exception));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(response.getBody()).isInstanceOf(Map.class);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertThat(body.get("error")).asString().contains("Unexpected error occurred");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("예상치 못한 예외를 INTERNAL_SERVER_ERROR로 처리해야 한다")
    void shouldHandleUnexpectedException() {
        // Given
        String path = "items/unexpected";
        RuntimeException exception = new RuntimeException("Unexpected error");

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.error(exception));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    assertThat(response.getBody()).isInstanceOf(Map.class);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertThat(body.get("error")).asString().contains("Unexpected error occurred");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("복잡한 경로와 쿼리 파라미터를 올바르게 빌드해야 한다")
    void shouldBuildComplexUrlCorrectly() {
        // Given
        String path = "/items";
        String queryParams = "archetype=Longsword&order=desc&limit=10&offset=20";
        Map<String, Object> mockResponse = Map.of("results", "complex data");
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(mockResponse);

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, queryParams);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        String expectedUrl = "https://api.darkerdb.com/v1/items?archetype=Longsword&order=desc&limit=10&offset=20";
        verify(requestHeadersUriSpec).uri(expectedUrl);
    }

    @Test
    @DisplayName("다중 슬래시가 포함된 경로를 올바르게 정리해야 한다")
    void shouldCleanupMultipleSlashesInPath() {
        // Given
        String path = "///items///search///";
        Map<String, Object> mockResponse = Map.of("data", "cleaned path");
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(mockResponse);

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, null);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(requestHeadersUriSpec).uri("https://api.darkerdb.com/v1/items///search///");
    }

    @Test
    @DisplayName("빈 쿼리 파라미터를 올바르게 처리해야 한다")
    void shouldHandleEmptyQueryParams() {
        // Given
        String path = "items";
        String queryParams = "";
        Map<String, Object> mockResponse = Map.of("data", "no params");
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(mockResponse);

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, queryParams);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(requestHeadersUriSpec).uri("https://api.darkerdb.com/v1/items");
    }

    @Test
    @DisplayName("공백만 포함된 쿼리 파라미터를 올바르게 처리해야 한다")
    void shouldHandleWhitespaceOnlyQueryParams() {
        // Given
        String path = "items";
        String queryParams = "   ";
        Map<String, Object> mockResponse = Map.of("data", "whitespace params");
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok(mockResponse);

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.just(expectedResponse));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, queryParams);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        verify(requestHeadersUriSpec).uri("https://api.darkerdb.com/v1/items");
    }

    @Test
    @DisplayName("다양한 HTTP 상태 코드를 올바르게 처리해야 한다")
    void shouldHandleVariousHttpStatusCodes() {
        // Given
        String path = "items/forbidden";
        WebClientResponseException exception = WebClientResponseException.create(
                403, "Forbidden", null, null, null);

        when(responseSpec.toEntity(Object.class))
                .thenReturn(Mono.error(exception));

        // When
        Mono<ResponseEntity<Object>> result = externalApiService.proxyGetRequest(path, null);

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(response.getBody()).isInstanceOf(Map.class);

                    @SuppressWarnings("unchecked")
                    Map<String, Object> body = (Map<String, Object>) response.getBody();
                    assertThat(body.get("error")).asString().contains("External API client error");
                    assertThat(body.get("originalStatus")).isEqualTo(403);
                    assertThat(body.get("originalMessage")).isEqualTo("Forbidden");
                })
                .verifyComplete();
    }
}
