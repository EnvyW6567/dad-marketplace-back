package org.envyw.dadmarketplace.infrastructure.repository.mapper;

import org.envyw.dadmarketplace.domain.User;
import org.envyw.dadmarketplace.infrastructure.persistence.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User fromEntity(UserEntity userEntity) {
        return User.builder()
                .userId(userEntity.getId())
                .username(userEntity.getUsername())
                .displayName(userEntity.getDisplayName())
                .avatarUrl(userEntity.getAvatarUrl())
                .email(userEntity.getEmail())
                .createdAt(userEntity.getCreatedAt())
                .build();
    }
}
