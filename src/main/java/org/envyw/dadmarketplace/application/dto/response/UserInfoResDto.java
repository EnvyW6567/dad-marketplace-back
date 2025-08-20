package org.envyw.dadmarketplace.application.dto.response;

import lombok.Builder;
import org.envyw.dadmarketplace.domain.User;

@Builder
public record UserInfoResDto(
        String avatarUrl,
        String displayName,
        String username
) {

    public static UserInfoResDto fromEntity(User user) {
        return UserInfoResDto.builder()
                .avatarUrl(user.getAvatarUrl())
                .displayName(user.getDisplayName())
                .username(user.getUsername())
                .build();
    }
}
