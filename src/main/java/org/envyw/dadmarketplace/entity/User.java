package org.envyw.dadmarketplace.entity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.envyw.dadmarketplace.security.dto.DiscordUserDto;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {

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

    public void updateInfo(DiscordUserDto discordUser) {
        this.username = discordUser.username();
        this.displayName = discordUser.displayName();
        this.email = discordUser.email();
        this.avatarUrl = discordUser.avatarUrl();
    }

    public static User fromDiscordUser(DiscordUserDto discordUser) {
        return User.builder()
                .discordId(discordUser.id())
                .username(discordUser.username())
                .displayName(discordUser.displayName())
                .email(discordUser.email())
                .avatarUrl(discordUser.avatarUrl())
                .build();
    }
}
