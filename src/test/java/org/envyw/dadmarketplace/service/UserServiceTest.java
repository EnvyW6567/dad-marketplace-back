package org.envyw.dadmarketplace.service;

import org.envyw.dadmarketplace.entity.User;
import org.envyw.dadmarketplace.repository.UserRepository;
import org.envyw.dadmarketplace.security.dto.DiscordUserDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("신규 사용자는 저장되어야 한다")
    void shouldSaveNewUser() {
        // Given
        DiscordUserDto discordUser = DiscordUserDto.builder()
                .id("123456789012345678")
                .username("newuser")
                .displayName("New User")
                .email("new@example.com")
                .avatarUrl("https://avatar.com/new.png")
                .build();

        User expectedUser = User.fromDiscordUser(discordUser);
        expectedUser = User.builder()
                .id(1L)
                .discordId(expectedUser.getDiscordId())
                .username(expectedUser.getUsername())
                .displayName(expectedUser.getDisplayName())
                .email(expectedUser.getEmail())
                .avatarUrl(expectedUser.getAvatarUrl())
                .build();

        when(userRepository.findByDiscordId(anyString())).thenReturn(Mono.empty());
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(expectedUser));

        // When
        Mono<User> result = userService.saveOrUpdateUser(discordUser);

        // Then
        StepVerifier.create(result)
                .assertNext(savedUser -> {
                    assertThat(savedUser.getId()).isEqualTo(1L);
                    assertThat(savedUser.getDiscordId()).isEqualTo("123456789012345678");
                    assertThat(savedUser.getUsername()).isEqualTo("newuser");
                    assertThat(savedUser.getDisplayName()).isEqualTo("New User");
                    assertThat(savedUser.getEmail()).isEqualTo("new@example.com");
                    assertThat(savedUser.getAvatarUrl()).isEqualTo("https://avatar.com/new.png");
                })
                .verifyComplete();

        verify(userRepository).findByDiscordId("123456789012345678");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("기존 사용자는 정보가 업데이트되어야 한다")
    void shouldUpdateExistingUser() {
        // Given
        User existingUser = User.builder()
                .id(1L)
                .discordId("123456789012345678")
                .username("olduser")
                .displayName("Old User")
                .email("old@example.com")
                .avatarUrl("https://avatar.com/old.png")
                .build();

        DiscordUserDto discordUser = DiscordUserDto.builder()
                .id("123456789012345678")
                .username("updateduser")
                .displayName("Updated User")
                .email("updated@example.com")
                .avatarUrl("https://avatar.com/updated.png")
                .build();

        User updatedUser = User.builder()
                .id(1L)
                .discordId("123456789012345678")
                .username("updateduser")
                .displayName("Updated User")
                .email("updated@example.com")
                .avatarUrl("https://avatar.com/updated.png")
                .build();

        when(userRepository.findByDiscordId(anyString())).thenReturn(Mono.just(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));

        // When
        Mono<User> result = userService.saveOrUpdateUser(discordUser);

        // Then
        StepVerifier.create(result)
                .assertNext(savedUser -> {
                    assertThat(savedUser.getId()).isEqualTo(1L);
                    assertThat(savedUser.getDiscordId()).isEqualTo("123456789012345678");
                    assertThat(savedUser.getUsername()).isEqualTo("updateduser");
                    assertThat(savedUser.getDisplayName()).isEqualTo("Updated User");
                    assertThat(savedUser.getEmail()).isEqualTo("updated@example.com");
                    assertThat(savedUser.getAvatarUrl()).isEqualTo("https://avatar.com/updated.png");
                })
                .verifyComplete();

        verify(userRepository).findByDiscordId("123456789012345678");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Discord ID로 사용자를 조회할 수 있어야 한다")
    void shouldFindUserByDiscordId() {
        // Given
        String discordId = "123456789012345678";
        User expectedUser = User.builder()
                .id(1L)
                .discordId(discordId)
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .avatarUrl("https://avatar.com/test.png")
                .build();

        when(userRepository.findByDiscordId(discordId)).thenReturn(Mono.just(expectedUser));

        // When
        Mono<User> result = userService.findByDiscordId(discordId);

        // Then
        StepVerifier.create(result)
                .assertNext(user -> {
                    assertThat(user.getId()).isEqualTo(1L);
                    assertThat(user.getDiscordId()).isEqualTo(discordId);
                    assertThat(user.getUsername()).isEqualTo("testuser");
                })
                .verifyComplete();

        verify(userRepository).findByDiscordId(discordId);
    }

    @Test
    @DisplayName("존재하지 않는 Discord ID 조회 시 빈 Mono를 반환해야 한다")
    void shouldReturnEmptyMonoWhenUserNotFound() {
        // Given
        String nonExistentDiscordId = "000000000000000000";
        when(userRepository.findByDiscordId(nonExistentDiscordId)).thenReturn(Mono.empty());

        // When
        Mono<User> result = userService.findByDiscordId(nonExistentDiscordId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verify(userRepository).findByDiscordId(nonExistentDiscordId);
    }

    @Test
    @DisplayName("사용자 정보가 실제로 변경된 경우에만 업데이트해야 한다")
    void shouldOnlyUpdateWhenUserInfoActuallyChanged() {
        // Given
        User existingUser = User.builder()
                .id(1L)
                .discordId("123456789012345678")
                .username("sameuser")
                .displayName("Same User")
                .email("same@example.com")
                .avatarUrl("https://avatar.com/same.png")
                .build();

        DiscordUserDto discordUser = DiscordUserDto.builder()
                .id("123456789012345678")
                .username("sameuser")
                .displayName("Same User")
                .email("same@example.com")
                .avatarUrl("https://avatar.com/same.png")
                .build();

        when(userRepository.findByDiscordId(anyString())).thenReturn(Mono.just(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(Mono.just(existingUser));

        // When
        Mono<User> result = userService.saveOrUpdateUser(discordUser);

        // Then
        StepVerifier.create(result)
                .assertNext(user -> {
                    assertThat(user.getUsername()).isEqualTo("sameuser");
                    assertThat(user.getDisplayName()).isEqualTo("Same User");
                })
                .verifyComplete();

        verify(userRepository).findByDiscordId("123456789012345678");
        verify(userRepository).save(any(User.class));
    }
}
