package org.envyw.dadmarketplace.infrastructure.persistence.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.envyw.dadmarketplace.domain.User;
import org.envyw.dadmarketplace.infrastructure.security.dto.DiscordUser;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Builder
@Getter
@AllArgsConstructor
@Table("users")
public class UserEntity {

    @Id
    private Long id;

    @NotBlank(message = "Discord ID는 필수입니다")
    @Size(max = 20, message = "Discord ID는 20자 이하여야 합니다")
    @Column("discord_id")
    private String discordId;

    @NotBlank(message = "사용자명은 필수입니다")
    @Size(max = 32, message = "사용자명은 32자 이하여야 합니다")
    @Column("username")
    private String username;

    @Size(max = 32, message = "표시명은 32자 이하여야 합니다")
    @Column("display_name")
    private String displayName;

    @Email(message = "올바른 이메일 형식이어야 합니다")
    @Size(max = 100, message = "이메일은 100자 이하여야 합니다")
    @Column("email")
    private String email;

    @Size(max = 255, message = "아바타 URL은 255자 이하여야 합니다")
    @Column("avatar_url")
    private String avatarUrl;

    @CreatedDate
    @Column("created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private LocalDateTime updatedAt;

    public static UserEntity fromDiscordUser(DiscordUser discordUser) {
        return UserEntity.builder()
                .discordId(discordUser.id())
                .username(discordUser.username())
                .displayName(discordUser.displayName())
                .email(discordUser.email())
                .avatarUrl(discordUser.avatarUrl())
                .build();
    }

    public static UserEntity fromUser(User user) {
        return UserEntity.builder()
                .id(user.getUserId())
                .discordId(user.getDiscordId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }
}
