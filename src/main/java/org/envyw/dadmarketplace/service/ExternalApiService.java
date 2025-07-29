package org.envyw.dadmarketplace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalApiService {

    @Value("${app.external-api.darkerdb-base-url}")
    private String EXTERNAL_API_BASE_URL;

    private final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;

    public Mono<ResponseEntity<Object>> proxyGetRequest(String path, String queryParams) {
        if (!StringUtils.hasText(path)) {
            log.warn("빈 경로로 외부 API 요청 시도");
            return Mono.just(ResponseEntity.badRequest()
                    .body(Map.of("error", "Path cannot be empty")));
        }

        String fullUrl = buildFullUrl(path, queryParams);
        log.info("외부 API 요청 시작: {}", fullUrl);

        return webClient.get()
                .uri(fullUrl)
                .retrieve()
                .toEntity(Object.class)
                .timeout(REQUEST_TIMEOUT)
                .doOnSuccess(response -> log.info("외부 API 요청 성공: {} - Status: {}",
                        fullUrl, response.getStatusCode()))
                .onErrorResume(this::handleError);
    }

    private String buildFullUrl(String path, String queryParams) {
        StringBuilder urlBuilder = new StringBuilder(EXTERNAL_API_BASE_URL);
        urlBuilder.append("/").append(path.replaceAll("^/+", ""));

        if (StringUtils.hasText(queryParams)) {
            urlBuilder.append("?").append(queryParams);
        }

        return urlBuilder.toString();
    }

    private Mono<ResponseEntity<Object>> handleError(Throwable throwable) {
        log.error("외부 API 요청 중 오류 발생", throwable);

        if (throwable instanceof WebClientResponseException responseException) {
            return handleWebClientResponseException(responseException);
        }

        if (throwable instanceof WebClientException) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "External API connection error: " + throwable.getMessage())));
        }

        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unexpected error occurred: " + throwable.getMessage())));
    }

    private Mono<ResponseEntity<Object>> handleWebClientResponseException(WebClientResponseException ex) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        String errorMessage;
        HttpStatus responseStatus;

        if (status.is4xxClientError()) {
            errorMessage = "External API client error: " + ex.getStatusText();
            responseStatus = status;
        } else if (status.is5xxServerError()) {
            errorMessage = "External API server error: " + ex.getStatusText();
            responseStatus = HttpStatus.BAD_GATEWAY;
        } else {
            errorMessage = "External API error: " + ex.getStatusText();
            responseStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return Mono.just(ResponseEntity.status(responseStatus)
                .body(Map.of(
                        "error", errorMessage,
                        "originalStatus", status.value(),
                        "originalMessage", ex.getStatusText()
                )));
    }
}
