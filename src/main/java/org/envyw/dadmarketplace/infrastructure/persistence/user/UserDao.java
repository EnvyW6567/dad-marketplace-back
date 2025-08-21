package org.envyw.dadmarketplace.infrastructure.persistence.user;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserDao extends ReactiveCrudRepository<UserEntity, Long> {
    Mono<UserEntity> findByDiscordId(String discordId);

    Mono<Long> findIdByDiscordId(String discordId);

    Mono<UserEntity> findById(Long id);
}
