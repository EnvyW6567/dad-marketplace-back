package org.envyw.dadmarketplace.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.application.dto.response.UserInfoResDto;
import org.envyw.dadmarketplace.application.port.out.AuthenticationPort;
import org.envyw.dadmarketplace.domain.AuthenticatedUser;
import org.envyw.dadmarketplace.infrastructure.persistence.User;
import org.envyw.dadmarketplace.infrastructure.repository.UserRepository;
import org.envyw.dadmarketplace.infrastructure.security.auth.exception.UnauthorizedException;
import org.envyw.dadmarketplace.infrastructure.security.auth.exception.UserNotFoundException;
import org.envyw.dadmarketplace.infrastructure.security.dto.DiscordUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final AuthenticationPort authContext;

    public Mono<Long> saveOrUpdateUser(DiscordUser discordUser) {
        Mono<User> user = userRepository.findByDiscordId(discordUser.id())
                .flatMap(existingUser -> updateUser(existingUser, discordUser))
                .switchIfEmpty(Mono.defer(() -> createUser(discordUser)));

        return user.map(User::getId);
    }

    public Mono<Long> findByDiscordId(String discordId) {
        // TODO: Validation and Throw Exception
        return userRepository.findIdByDiscordId(discordId);
    }

    public Mono<UserInfoResDto> getCurrentUserInfo() {
        return authContext.getCurrentUser()
                .filter(AuthenticatedUser::isAuthenticated)
                .switchIfEmpty(Mono.error(new UnauthorizedException("사용자가 인증되지 않았습니다")))
                .flatMap(authUser ->
                        userRepository.findById(authUser.getUserId())
                                .map(UserInfoResDto::fromEntity)
                                .switchIfEmpty(Mono.error(new UserNotFoundException("사용자를 찾을 수 없습니다")))
                );
    }

    private Mono<User> updateUser(User user, DiscordUser discordUser) {
        log.info("기존 사용자 정보 업데이트: discordId={}, username={}",
                discordUser.id(), discordUser.username());

        user.updateInfo(discordUser);
        return userRepository.save(user);
    }

    private Mono<User> createUser(DiscordUser discordUser) {
        log.info("신규 사용자 저장: discordId={}, username={}",
                discordUser.id(), discordUser.username());

        User newUser = User.fromDiscordUser(discordUser);
        return userRepository.save(newUser);

    }
}
