package org.envyw.dadmarketplace.service;

import lombok.RequiredArgsConstructor;
import org.envyw.dadmarketplace.dto.request.RefreshReqDto;
import org.envyw.dadmarketplace.dto.response.RefreshResDto;
import org.envyw.dadmarketplace.security.jwt.JwtTokenService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenService jwtTokenService;
    private final UserService userService;

    public Mono<RefreshResDto> refresh(RefreshReqDto refreshReq) {
        String refreshToken = refreshReq.refreshToken();

        Authentication authentication = jwtTokenService.authenticate(refreshToken);

        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            return Mono.error(new IllegalArgumentException("유효하지 않은 인증 토큰입니다"));
        }

        Jwt jwt = jwtAuth.getToken();
        String discordId = jwtTokenService.extractDiscordId(jwt);

        return userService.findByDiscordId(discordId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("사용자를 찾을 수 없습니다")))
                .map(user -> jwtTokenService.renewAccessToken(jwt, user))
                .map(RefreshResDto::new);
    }

}
