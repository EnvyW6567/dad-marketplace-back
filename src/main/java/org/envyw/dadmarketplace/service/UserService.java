package org.envyw.dadmarketplace.service;

import org.envyw.dadmarketplace.entity.User;
import org.envyw.dadmarketplace.security.dto.DiscordUserDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    public Mono<User> saveOrUpdateUser(DiscordUserDto discordUser) {
        return Mono.empty();
    }

    public Mono<User> findByDiscordId(String discordId) {
        return Mono.empty();
    }

}
