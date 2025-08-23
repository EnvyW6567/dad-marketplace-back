package org.envyw.dadmarketplace.application.service;

import org.envyw.dadmarketplace.application.repository.UserRepository;
import org.envyw.dadmarketplace.domain.User;
import org.envyw.dadmarketplace.infrastructure.security.dto.DiscordUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User createUserByDiscordUserId(String discordUserId) {
        return User.builder()
                .userId(123L)
                .discordId(discordUserId)
                .email("email@email.com")
                .avatarUrl("https://avatar.url")
                .username("testUsername")
                .displayName("testDisplayName")
                .build();
    }

    private DiscordUser createDiscordUser(String discordUserid) {
        return DiscordUser.builder()
                .id(discordUserid)
                .email("email@email.com")
                .avatarUrl("https://avatar.url")
                .username("testUsername")
                .displayName("testDisplayName")
                .build();
    }

    @Nested
    @DisplayName("saveOrUpdateUser 메서드")
    class SaveOrUpdateUserMethod {

        @Test
        @DisplayName("새로운 DiscordUser에 대해서는 새로운 User를 저장")
        void shouldSaveNewUserWithNewDiscordUser() {
            // Given
            String newDiscordUserId = "123";
            DiscordUser newDiscordUser = createDiscordUser(newDiscordUserId);
            User newUser = createUserByDiscordUserId(newDiscordUserId);

            when(userRepository.findByDiscordId(newDiscordUserId)).thenReturn(Mono.empty());
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(newUser));

            // When
            Mono<Long> newUserId = userService.saveOrUpdateUser(newDiscordUser);

            // Then
            StepVerifier.create(newUserId)
                    .assertNext(userId -> assertThat(userId).isEqualTo(newUser.getUserId()))
                    .verifyComplete();

            verify(userRepository).findByDiscordId(newDiscordUserId);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("이미 있는 DiscordUser에 대해서는 디스코드 정보를 업데이트")
        void shouldUpdateUserWithExistDiscordUser() {
            // Given
            String existDiscorUserId = "123";
            DiscordUser existDiscordUser = createDiscordUser(existDiscorUserId);
            User existUser = createUserByDiscordUserId(existDiscorUserId);

            when(userRepository.findByDiscordId(existDiscorUserId)).thenReturn(Mono.just(existUser));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(existUser));

            // When
            Mono<Long> existUserId = userService.saveOrUpdateUser(existDiscordUser);

            // Then
            StepVerifier.create(existUserId)
                    .assertNext(userId -> assertThat(userId).isEqualTo(existUser.getUserId()))
                    .verifyComplete();

            verify(userRepository).findByDiscordId(existDiscorUserId);
            verify(userRepository).save(any(User.class));
        }
    }
}
