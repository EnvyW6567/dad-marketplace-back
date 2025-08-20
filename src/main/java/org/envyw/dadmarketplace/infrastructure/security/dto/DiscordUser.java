package org.envyw.dadmarketplace.infrastructure.security.dto;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record DiscordUser(
        @NonNull String id,
        @NonNull String username,
        @NonNull String avatarUrl,
        @NonNull String displayName,
        String email
) {
}
