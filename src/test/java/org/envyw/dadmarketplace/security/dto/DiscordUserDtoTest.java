package org.envyw.dadmarketplace.security.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DiscordUserDtoTest {

    @Test
    @DisplayName("필수 필드로 DiscordUserDto를 생성할 수 있어야 한다")
    public void shouldCreateDtoWithRequiredFields() {
        // given
        String id = "12345678";
        String username = "테스트유저";
        String avatarUrl = "https://cdn.discordapp.com/avatars/12345678/asdbsd.png";
        String displayName = "Test001";

        // when
        DiscordUserDto dto = new DiscordUserDto(id, username, avatarUrl, null, displayName);

        // then
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.username()).isEqualTo(username);
        assertThat(dto.avatarUrl()).isEqualTo(avatarUrl);
        assertThat(dto.email()).isNull();
        assertThat(dto.displayName()).isEqualTo(displayName);
    }

    @Test
    @DisplayName("모든 필드로 DiscordUserDto를 생성할 수 있어야 한다")
    public void shouldCreateDtoWithAllFields() {
        // given
        String id = "12345678";
        String username = "테스트유저";
        String avatarUrl = "https://cdn.discordapp.com/avatars/12345678/abcdef.png";
        String email = "test@example.com";
        String displayName = "Test001";

        // when
        DiscordUserDto dto = new DiscordUserDto(id, username, avatarUrl, email, displayName);

        // then
        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.username()).isEqualTo(username);
        assertThat(dto.avatarUrl()).isEqualTo(avatarUrl);
        assertThat(dto.email()).isEqualTo(email);
        assertThat(dto.displayName()).isEqualTo(displayName);
    }

    @Test
    @DisplayName("ID가 null이면 예외가 발생해야 한다")
    public void shouldThrowExceptionWhenIdIsNull() {
        // when, then
        assertThatThrownBy(() -> new DiscordUserDto(null, "테스트유저", null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("사용자명이 null이면 예외가 발생해야 한다")
    public void shouldThrowExceptionWhenUsernameIsNull() {
        // when, then
        assertThatThrownBy(() -> new DiscordUserDto("12345678", null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("아바타 URL이 null이면 예외가 발생해야 한다")
    public void shouldThrowExceptionWhenAvatarIsNull() {
        // when, then
        assertThatThrownBy(() -> new DiscordUserDto("12345678", "1234", null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
