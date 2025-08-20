package org.envyw.dadmarketplace.application.port.out;

import org.envyw.dadmarketplace.domain.AuthenticatedUser;
import reactor.core.publisher.Mono;

public interface AuthenticationPort {
    Mono<AuthenticatedUser> getCurrentUser();

    Mono<Boolean> isAuthenticated();
}
