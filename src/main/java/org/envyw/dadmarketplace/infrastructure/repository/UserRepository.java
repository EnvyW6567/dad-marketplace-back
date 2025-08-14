package org.envyw.dadmarketplace.infrastructure.repository;

import org.envyw.dadmarketplace.infrastructure.persistence.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByDiscordId(String discordId);

    Mono<Long> findIdByDiscordId(String discordId);

    Mono<User> findById(Long id);
}
