package org.envyw.dadmarketplace.repository;

import org.envyw.dadmarketplace.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends ReactiveCrudRepository<User, Long> {
    Mono<User> findByDiscordId(String discordId);
}
