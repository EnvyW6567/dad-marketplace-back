package org.envyw.dadmarketplace.infrastructure.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.infrastructure.security.jwt.exception.JwtAuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.envyw.dadmarketplace.infrastructure.security.jwt.constants.JwtConstants.AUTHORITIES_CLAIM;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticator {
    private final JwtDecoder jwtDecoder;

    public JwtAuthenticationToken authenticate(String tokenValue) {
        try {
            Jwt jwt = jwtDecoder.decode(tokenValue);

            List<String> authorities = jwt.getClaimAsStringList(AUTHORITIES_CLAIM);
            List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt, grantedAuthorities);

            log.info("JWT 토큰 인증 성공: subject={}", jwt.getSubject());

            return authentication;

        } catch (JwtException e) {
            log.error("JWT 토큰 인증 실패: {}", e.getMessage());
            throw new JwtAuthenticationException("JWT 토큰 인증에 실패했습니다", e);
        }
    }

}
