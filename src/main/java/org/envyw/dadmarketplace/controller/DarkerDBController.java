package org.envyw.dadmarketplace.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.service.ExternalApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/darkerdb")
@RequiredArgsConstructor
@Slf4j
public class DarkerDBController {

    private final ExternalApiService externalApiService;

    @GetMapping("/**")
    public Mono<ResponseEntity<Object>> proxyGetRequest(ServerHttpRequest request) {
        String requestPath = request.getPath().value();
        String proxyBasePath = "/api/proxy/darkerdb";

        String targetPath = requestPath.substring(proxyBasePath.length());
        if (targetPath.startsWith("/")) {
            targetPath = targetPath.substring(1);
        }

        String queryString = request.getURI().getQuery();

        log.info("프록시 요청 수신: path={}, queryString={}", targetPath, queryString);

        return externalApiService.proxyGetRequest(targetPath, queryString);
    }
}
