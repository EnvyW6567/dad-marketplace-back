package org.envyw.dadmarketplace.fixture;

import org.envyw.dadmarketplace.entity.User;
import org.envyw.dadmarketplace.security.dto.DiscordUserDto;

public class UserTestDataBuilder {

    public static User.UserBuilder baseUser() {
        return User.builder()
                .id(1L)
                .discordId("123456789012345678")
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .avatarUrl("https://avatar.com/test.png");
    }

    public static User.UserBuilder userWith(String discordId) {
        return User.builder()
                .id(1L)
                .discordId(discordId)
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .avatarUrl("https://avatar.com/test.png");
    }

    public static DiscordUserDto baseDiscordUser() {
        return new DiscordUserDto(
                "123456789012345678",
                "testuser",
                "https://avatar.com/test.png",
                "test@example.com",
                "Test User"
        );
    }

    public static DiscordUserDto discordUserWith(String username, String email) {
        return new DiscordUserDto(
                "123456789012345678",
                username,
                "https://avatar.com/test.png",
                email,
                "Updated User"
        );
    }
}
