package org.envyw.dadmarketplace.entity;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.envyw.dadmarketplace.security.dto.DiscordUserDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User 엔티티 테스트")
class UserTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("유효한 사용자 정보로 User 엔티티를 생성할 수 있다")
    void shouldCreateValidUserEntity() {
        // Given
        String discordId = "123456789012345678";
        String username = "testuser";
        String displayName = "Test User";
        String email = "test@example.com";
        String avatarUrl = "https://cdn.discordapp.com/avatars/123/abc.png";

        // When
        User user = User.builder()
                .discordId(discordId)
                .username(username)
                .displayName(displayName)
                .email(email)
                .avatarUrl(avatarUrl)
                .build();

        // Then
        assertThat(user.getDiscordId()).isEqualTo(discordId);
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getDisplayName()).isEqualTo(displayName);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getAvatarUrl()).isEqualTo(avatarUrl);
        assertThat(user.getCreatedAt()).isNull(); // DB에서 자동 생성
        assertThat(user.getUpdatedAt()).isNull(); // DB에서 자동 생성
    }

    @Test
    @DisplayName("Discord ID가 null이면 유효성 검증에 실패한다")
    void shouldFailValidationWhenDiscordIdIsNull() {
        // Given
        User user = User.builder()
                .discordId(null)
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Discord ID는 필수입니다");
    }

    @Test
    @DisplayName("Discord ID가 빈 문자열이면 유효성 검증에 실패한다")
    void shouldFailValidationWhenDiscordIdIsEmpty() {
        // Given
        User user = User.builder()
                .discordId("")
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Discord ID는 필수입니다");
    }

    @Test
    @DisplayName("Discord ID가 20자를 초과하면 유효성 검증에 실패한다")
    void shouldFailValidationWhenDiscordIdIsTooLong() {
        // Given
        String longDiscordId = "123456789012345678901"; // 21자
        User user = User.builder()
                .discordId(longDiscordId)
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("Discord ID는 20자 이하여야 합니다");
    }

    @Test
    @DisplayName("사용자명이 null이면 유효성 검증에 실패한다")
    void shouldFailValidationWhenUsernameIsNull() {
        // Given
        User user = User.builder()
                .discordId("123456789012345678")
                .username(null)
                .displayName("Test User")
                .email("test@example.com")
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("사용자명은 필수입니다");
    }

    @Test
    @DisplayName("사용자명이 32자를 초과하면 유효성 검증에 실패한다")
    void shouldFailValidationWhenUsernameIsTooLong() {
        // Given
        String longUsername = "a".repeat(33); // 33자
        User user = User.builder()
                .discordId("123456789012345678")
                .username(longUsername)
                .displayName("Test User")
                .email("test@example.com")
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("사용자명은 32자 이하여야 합니다");
    }

    @Test
    @DisplayName("잘못된 이메일 형식이면 유효성 검증에 실패한다")
    void shouldFailValidationWhenEmailFormatIsInvalid() {
        // Given
        User user = User.builder()
                .discordId("123456789012345678")
                .username("testuser")
                .displayName("Test User")
                .email("invalid-email")
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("올바른 이메일 형식이어야 합니다");
    }

    @Test
    @DisplayName("이메일이 100자를 초과하면 유효성 검증에 실패한다")
    void shouldFailValidationWhenEmailIsTooLong() {
        // Given
        String longEmail = "a".repeat(64) + "@veryveryveryveryveryverylongdomain.com";
        User user = User.builder()
                .discordId("123456789012345678")

                .username("testuser")
                .displayName("Test User")
                .email(longEmail)
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
                .isEqualTo("이메일은 100자 이하여야 합니다");
    }

    @Test
    @DisplayName("사용자 정보를 업데이트할 수 있다")
    void shouldUpdateUserInformation() {
        // Given
        User user = User.builder()
                .discordId("123456789012345678")
                .username("olduser")
                .displayName("Old User")
                .email("old@example.com")
                .avatarUrl("https://old-avatar.com/old.png")
                .build();

        DiscordUserDto discordUser = DiscordUserDto.builder()
                .id("123456789012345678")
                .username("newuser")
                .displayName("New User")
                .email("new@example.com")
                .avatarUrl("https://new-avatar.com/new.png")
                .build();

        // When
        user.updateInfo(discordUser);

        // Then
        assertThat(user.getUsername()).isEqualTo("newuser");
        assertThat(user.getDisplayName()).isEqualTo("New User");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getAvatarUrl()).isEqualTo("https://new-avatar.com/new.png");
    }

    @Test
    @DisplayName("선택적 필드들이 null이어도 유효성 검증을 통과한다")
    void shouldPassValidationWithOptionalFieldsAsNull() {
        // Given
        User user = User.builder()
                .discordId("123456789012345678")
                .username("testuser")
                .displayName(null) // 선택적 필드
                .email(null) // 선택적 필드
                .avatarUrl(null) // 선택적 필드
                .build();

        // When
        Set<ConstraintViolation<User>> violations = validator.validate(user);

        // Then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("FromDiscordUser 메서드는 올바른 DiscordUserDto 입력에 대해 User 객체를 생성해야한다")
    void fromDiscordUserTest() {
        // Given
        String discordId = "123456789012345678";
        String username = "testuser";
        String displayName = "Test User";
        String email = "test@example.com";
        String avatarUrl = "https://cdn.discordapp.com/avatars/123/abc.png";

        DiscordUserDto discordUser = DiscordUserDto.builder()
                .id(discordId)
                .username(username)
                .displayName(displayName)
                .email(email)
                .avatarUrl(avatarUrl)
                .build();

        // When
        User user = User.fromDiscordUser(discordUser);

        // Then
        assertThat(user.getDiscordId()).isEqualTo(discordId);
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getDisplayName()).isEqualTo(displayName);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getAvatarUrl()).isEqualTo(avatarUrl);
        assertThat(user.getCreatedAt()).isNull(); // DB에서 자동 생성
        assertThat(user.getUpdatedAt()).isNull(); // DB에서 자동 생성
    }
}
