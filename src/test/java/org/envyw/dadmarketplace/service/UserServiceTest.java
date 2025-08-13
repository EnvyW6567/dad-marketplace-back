package org.envyw.dadmarketplace.service;

import org.envyw.dadmarketplace.application.service.UserService;
import org.envyw.dadmarketplace.fixture.UserTestDataBuilder;
import org.envyw.dadmarketplace.infrastructure.persistence.User;
import org.envyw.dadmarketplace.infrastructure.repository.UserRepository;
import org.envyw.dadmarketplace.infrastructure.security.dto.DiscordUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 테스트 - 비즈니스 로직 중심")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private UserService userService;

    @Nested
    @DisplayName("saveOrUpdateUser - 핵심 비즈니스 로직")
    class SaveOrUpdateUserTest {

        @Test
        @DisplayName("신규 사용자 등록 시 올바른 User 객체 반환")
        void saveNewUser_ReturnsCorrectUser() {
            // Given
            DiscordUser discordUser = UserTestDataBuilder.baseDiscordUser();
            User expectedUser = UserTestDataBuilder.baseUser().build();

            when(userRepository.findByDiscordId(discordUser.id())).thenReturn(Mono.empty());
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(expectedUser));

            // When
            Mono<User> result = userService.saveOrUpdateUser(discordUser);

            // Then - 결과값 검증에 집중
            StepVerifier.create(result)
                    .assertNext(user -> {
                        assertThat(user.getDiscordId()).isEqualTo(discordUser.id());
                        assertThat(user.getUsername()).isEqualTo(discordUser.username());
                        assertThat(user.getEmail()).isEqualTo(discordUser.email());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("기존 사용자 정보 업데이트 시 변경된 정보 반영")
        void updateExistingUser_ReturnsUpdatedUser() {
            // Given
            User existingUser = UserTestDataBuilder.baseUser()
                    .username("oldname")
                    .email("old@email.com")
                    .build();

            DiscordUser updatedInfo = UserTestDataBuilder.discordUserWith("newname", "new@email.com");

            User updatedUser = UserTestDataBuilder.baseUser()
                    .username("newname")
                    .email("new@email.com")
                    .build();

            when(userRepository.findByDiscordId(updatedInfo.id())).thenReturn(Mono.just(existingUser));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(updatedUser));

            // When
            Mono<User> result = userService.saveOrUpdateUser(updatedInfo);

            // Then
            StepVerifier.create(result)
                    .assertNext(user -> {
                        assertThat(user.getUsername()).isEqualTo("newname");
                        assertThat(user.getEmail()).isEqualTo("new@email.com");
                        assertThat(user.getDiscordId()).isEqualTo(updatedInfo.id());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("동일한 정보로 업데이트해도 정상 처리")
        void updateWithSameInfo_ProcessesNormally() {
            // Given
            DiscordUser discordUser = UserTestDataBuilder.baseDiscordUser();
            User existingUser = UserTestDataBuilder.baseUser()
                    .username(discordUser.username())
                    .email(discordUser.email())
                    .displayName(discordUser.displayName())
                    .build();

            when(userRepository.findByDiscordId(discordUser.id())).thenReturn(Mono.just(existingUser));
            when(userRepository.save(any(User.class))).thenReturn(Mono.just(existingUser));

            // When
            Mono<User> result = userService.saveOrUpdateUser(discordUser);

            // Then
            StepVerifier.create(result)
                    .assertNext(user -> {
                        assertThat(user.getUsername()).isEqualTo(discordUser.username());
                        assertThat(user.getEmail()).isEqualTo(discordUser.email());
                    })
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("findByDiscordId - 조회 로직")
    class FindByDiscordIdTest {

        @Test
        @DisplayName("존재하는 사용자 조회 성공")
        void findExistingUser_ReturnsUser() {
            // Given
            String discordId = "123456789";
            User expectedUser = UserTestDataBuilder.userWith(discordId).build();
            when(userRepository.findByDiscordId(discordId)).thenReturn(Mono.just(expectedUser));

            // When
            Mono<User> result = userService.findByDiscordId(discordId);

            // Then
            StepVerifier.create(result)
                    .assertNext(user -> assertThat(user.getDiscordId()).isEqualTo(discordId))
                    .verifyComplete();
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 빈 결과")
        void findNonExistentUser_ReturnsEmpty() {
            // Given
            String discordId = "nonexistent";
            when(userRepository.findByDiscordId(discordId)).thenReturn(Mono.empty());

            // When & Then
            StepVerifier.create(userService.findByDiscordId(discordId))
                    .verifyComplete();
        }
    }

    @Nested
    @DisplayName("에러 처리")
    class ErrorHandlingTest {

        @Test
        @DisplayName("Repository 에러 시 예외 전파")
        void repositoryError_PropagatesException() {
            // Given
            DiscordUser discordUser = UserTestDataBuilder.baseDiscordUser();
            when(userRepository.findByDiscordId(discordUser.id()))
                    .thenReturn(Mono.error(new RuntimeException("DB 연결 실패")));

            // When & Then
            StepVerifier.create(userService.saveOrUpdateUser(discordUser))
                    .expectErrorMatches(throwable ->
                            throwable instanceof RuntimeException &&
                                    throwable.getMessage().equals("DB 연결 실패"))
                    .verify();
        }
    }
}
