package org.envyw.dadmarketplace.infrastructure.persistence.user;

import org.envyw.dadmarketplace.domain.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User fromEntity(UserEntity userEntity) {
        return User.builder()
                .userId(userEntity.getId())
                .discordId(userEntity.getDiscordId())
                .username(userEntity.getUsername())
                .displayName(userEntity.getDisplayName())
                .avatarUrl(userEntity.getAvatarUrl())
                .email(userEntity.getEmail())
                .createdAt(userEntity.getCreatedAt())
                .build();
    }
}
