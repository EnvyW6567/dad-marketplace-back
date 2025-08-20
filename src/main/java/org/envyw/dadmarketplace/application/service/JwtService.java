package org.envyw.dadmarketplace.application.service;

import lombok.RequiredArgsConstructor;
import org.envyw.dadmarketplace.application.dto.request.RefreshReqDto;
import org.envyw.dadmarketplace.application.dto.response.RefreshResDto;
import org.envyw.dadmarketplace.infrastructure.security.jwt.JwtAdapter;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtAdapter jwtAdapter;


    public Mono<RefreshResDto> refresh(RefreshReqDto refreshReq) {
        String refreshToken = refreshReq.refreshToken();
        String accessToken = jwtAdapter.refresh(refreshToken);

        return Mono.just(new RefreshResDto(accessToken));
    }
}
