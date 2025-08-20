package org.envyw.dadmarketplace.domain;

import lombok.Builder;
import lombok.Getter;
import org.envyw.dadmarketplace.infrastructure.security.dto.DiscordUser;

import java.time.LocalDateTime;

@Getter
@Builder
public class User {
    private final Long userId;
    private final LocalDateTime createdAt;
    private final String discordId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String email;

    public void updateByDiscordUser(DiscordUser discordUser) {
        this.avatarUrl = discordUser.avatarUrl();
        this.displayName = discordUser.displayName();
        this.username = discordUser.username();
        this.email = discordUser.email();
    }
}
