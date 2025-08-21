package org.envyw.dadmarketplace.infrastructure.persistence.user;

import lombok.RequiredArgsConstructor;
import org.envyw.dadmarketplace.application.repository.UserRepository;
import org.envyw.dadmarketplace.domain.User;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Repository
public class UserR2dbcRepository implements UserRepository {
    private final UserDao userDao;
    private final UserMapper userMapper;

    @Override
    public Mono<User> save(User user) {
        UserEntity userEntity = UserEntity.fromUser(user);

        Mono<UserEntity> savedUserEntity = userDao.save(userEntity);

        return savedUserEntity.map(userMapper::fromEntity);
    }

    @Override
    public Mono<User> findByDiscordId(String discordId) {
        Mono<UserEntity> userEntity = userDao.findByDiscordId(discordId);

        return userEntity.map(userMapper::fromEntity);
    }

    @Override
    public Mono<User> findById(Long id) {
        Mono<UserEntity> userEntity = userDao.findById(id);

        return userEntity.map(userMapper::fromEntity);
    }

    @Override
    public Mono<Long> findIdByDiscordId(String discordId) {
        return userDao.findIdByDiscordId(discordId);
    }
}
