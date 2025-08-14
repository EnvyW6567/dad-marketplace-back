package org.envyw.dadmarketplace.application.dto.response;

import lombok.Builder;
import org.envyw.dadmarketplace.infrastructure.persistence.User;

@Builder
public record UserInfoResDto(
        String avatarUrl,
        String displayName,
        String username
) {

    // TODO: Infra's Persistence User >> Domain User / 변경 필요
    public static UserInfoResDto fromEntity(User user) {
        return UserInfoResDto.builder()
                .avatarUrl(user.getAvatarUrl())
                .displayName(user.getDisplayName())
                .username(user.getUsername())
                .build();
    }
}
