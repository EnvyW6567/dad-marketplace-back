package org.envyw.dadmarketplace.domain.factory;

import org.envyw.dadmarketplace.domain.User;
import org.envyw.dadmarketplace.infrastructure.security.dto.DiscordUser;

public class UserFactory {
    public static User fromDiscordUser(DiscordUser discordUser) {
        return User.builder()
                .discordId(discordUser.id())
                .username(discordUser.username())
                .displayName(discordUser.displayName())
                .avatarUrl(discordUser.avatarUrl())
                .email(discordUser.email())
                .build();
    }
}
