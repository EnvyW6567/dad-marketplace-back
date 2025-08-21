package org.envyw.dadmarketplace.infrastructure.repository.dao;

import org.envyw.dadmarketplace.infrastructure.persistence.UserEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserDao extends ReactiveCrudRepository<UserEntity, Long> {
    Mono<UserEntity> findByDiscordId(String discordId);

    Mono<Long> findIdByDiscordId(String discordId);

    Mono<UserEntity> findById(Long id);
}
