package org.envyw.dadmarketplace.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.infrastructure.persistence.User;
import org.envyw.dadmarketplace.infrastructure.repository.UserRepository;
import org.envyw.dadmarketplace.infrastructure.security.dto.DiscordUser;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

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
