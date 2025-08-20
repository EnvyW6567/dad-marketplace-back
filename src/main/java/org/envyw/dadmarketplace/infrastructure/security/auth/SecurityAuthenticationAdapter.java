package org.envyw.dadmarketplace.infrastructure.security.auth;

import lombok.RequiredArgsConstructor;
import org.envyw.dadmarketplace.application.port.out.AuthenticationPort;
import org.envyw.dadmarketplace.domain.AuthenticatedUser;
import org.envyw.dadmarketplace.domain.factory.AuthenticatedUserFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SecurityAuthenticationAdapter implements AuthenticationPort {
    @Override
    public Mono<AuthenticatedUser> getCurrentUser() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(this::convertToAuthenticatedUser)
                .defaultIfEmpty(AuthenticatedUserFactory.anonymous());
    }

    @Override
    public Mono<Boolean> isAuthenticated() {
        return getCurrentUser()
                .map(AuthenticatedUser::isAuthenticated);
    }

    private AuthenticatedUser convertToAuthenticatedUser(Authentication authentication) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String userId = jwtAuth.getToken().getSubject();
            Set<String> authorities = jwtAuth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toSet());

            return AuthenticatedUserFactory.authenticated(Long.valueOf(userId), authorities);
        }

        return AuthenticatedUserFactory.anonymous();
    }
}
