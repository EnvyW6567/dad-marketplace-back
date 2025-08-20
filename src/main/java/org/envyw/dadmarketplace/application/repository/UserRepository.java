package org.envyw.dadmarketplace.application.repository;

import org.envyw.dadmarketplace.domain.User;
import reactor.core.publisher.Mono;

public interface UserRepository {
    Mono<User> save(User user);

    Mono<User> findByDiscordId(String discordId);

    Mono<Long> findIdByDiscordId(String discordId);

    Mono<User> findById(Long id);
}
