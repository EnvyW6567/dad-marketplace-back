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

    /**
     * 사용자 정보를 업데이트합니다.
     *
     * @param username    새로운 사용자명
     * @param displayName 새로운 표시명
     * @param email       새로운 이메일
     * @param avatarUrl   새로운 아바타 URL
     */
    public void updateInfo(String username, String displayName, String email, String avatarUrl) {
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    /**
     * Discord OAuth2 정보로부터 User 엔티티를 생성합니다.
     *
     * @param discordUser 디스코드 OAuth2 인증 후 얻은 사용자 정보
     * @return 생성된 User 엔티티
     */
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
