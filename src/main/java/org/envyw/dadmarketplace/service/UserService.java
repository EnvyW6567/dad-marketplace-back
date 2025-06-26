package org.envyw.dadmarketplace.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.entity.User;
import org.envyw.dadmarketplace.repository.UserRepository;
import org.envyw.dadmarketplace.security.dto.DiscordUserDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public Mono<User> saveOrUpdateUser(DiscordUserDto discordUser) {
        return userRepository.findByDiscordId(discordUser.id())
                .flatMap(existingUser -> {
                    log.info("기존 사용자 정보 업데이트: discordId={}, username={}",
                            discordUser.id(), discordUser.username());

                    existingUser.updateInfo(discordUser);

                    return userRepository.save(existingUser);
                })
                .switchIfEmpty(
                        Mono.defer(() -> {
                            log.info("신규 사용자 저장: discordId={}, username={}",
                                    discordUser.id(), discordUser.username());

                            User newUser = User.fromDiscordUser(discordUser);

                            return userRepository.save(newUser);
                        })
                );
    }

    public Mono<User> findByDiscordId(String discordId) {
        return userRepository.findByDiscordId(discordId);
    }

}
