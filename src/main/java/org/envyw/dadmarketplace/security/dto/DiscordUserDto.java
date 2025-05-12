package org.envyw.dadmarketplace.security.dto;

import lombok.Builder;
import lombok.NonNull;

@Builder
public record DiscordUserDto(
        @NonNull String id,
        @NonNull String username,
        @NonNull String avatarUrl,
        String email
) {
}
