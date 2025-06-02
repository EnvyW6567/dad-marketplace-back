package org.envyw.dadmarketplace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class WebConfig {

    @Bean
    public RouterFunction<ServerResponse> faviconRouter() {
        return RouterFunctions
                .route(RequestPredicates.GET("/favicon.ico"),
                        request -> ServerResponse.ok().contentType(MediaType.IMAGE_JPEG).bodyValue(new byte[0]));
    }
}
