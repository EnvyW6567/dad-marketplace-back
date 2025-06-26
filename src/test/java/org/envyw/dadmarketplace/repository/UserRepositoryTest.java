package org.envyw.dadmarketplace.repository;

import io.r2dbc.spi.ConnectionFactory;
import org.envyw.dadmarketplace.config.R2dbcConfig;
import org.envyw.dadmarketplace.entity.User;
import org.envyw.dadmarketplace.security.dto.DiscordUserDto;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DataR2dbcTest
@Import(R2dbcConfig.class)
@Testcontainers
@DisplayName("UserRepository 테스트")
class UserRepositoryTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpassword")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:mysql://" + mysql.getHost() + ":" + mysql.getFirstMappedPort() + "/testdb");
        registry.add("spring.r2dbc.username", mysql::getUsername);
        registry.add("spring.r2dbc.password", mysql::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @BeforeAll
    static void initSchema(@Autowired ConnectionFactory connectionFactory) {
        // 스키마 초기화
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema/schema.sql"));

        initializer.setDatabasePopulator(populator);
        initializer.afterPropertiesSet();
    }

    @Test
    @DisplayName("사용자를 저장할 수 있다")
    void shouldSaveUser() {
        // Given
        User user = User.builder()
                .discordId("123456789012345678")
                .username("testuser")
                .displayName("Test User")
                .email("test@example.com")
                .avatarUrl("https://avatar.com/test.png")
                .build();

        // When
        Mono<User> savedUserMono = userRepository.save(user);

        // Then
        StepVerifier.create(savedUserMono)
                .assertNext(savedUser -> {
                    assertThat(savedUser.getId()).isNotNull();
                    assertThat(savedUser.getDiscordId()).isEqualTo("123456789012345678");
                    assertThat(savedUser.getUsername()).isEqualTo("testuser");
                    assertThat(savedUser.getDisplayName()).isEqualTo("Test User");
                    assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
                    assertThat(savedUser.getAvatarUrl()).isEqualTo("https://avatar.com/test.png");
                    assertThat(savedUser.getCreatedAt()).isNotNull();
                    assertThat(savedUser.getUpdatedAt()).isNotNull();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Discord ID로 사용자를 조회할 수 있다")
    void shouldFindUserByDiscordId() {
        // Given
        User user = User.builder()
                .discordId("987654321098765432")
                .username("finduser")
                .displayName("Find User")
                .email("find@example.com")
                .build();

        // When
        Mono<User> result = userRepository.save(user)
                .then(userRepository.findByDiscordId("987654321098765432"));

        // Then
        StepVerifier.create(result)
                .assertNext(foundUser -> {
                    assertThat(foundUser.getDiscordId()).isEqualTo("987654321098765432");
                    assertThat(foundUser.getUsername()).isEqualTo("finduser");
                    assertThat(foundUser.getDisplayName()).isEqualTo("Find User");
                    assertThat(foundUser.getEmail()).isEqualTo("find@example.com");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("존재하지 않는 Discord ID로 조회하면 빈 Mono를 반환한다")
    void shouldReturnEmptyMonoWhenDiscordIdNotFound() {
        // Given
        String nonExistentDiscordId = "000000000000000000";

        // When
        Mono<User> result = userRepository.findByDiscordId(nonExistentDiscordId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    @DisplayName("사용자 정보를 업데이트할 수 있다")
    void shouldUpdateUser() {
        // Given
        User user = User.builder()
                .discordId("111111111111111111")
                .username("originaluser")
                .displayName("Original User")
                .email("original@example.com")
                .avatarUrl("https://avatar.com/original.png")
                .build();

        DiscordUserDto discordUser = DiscordUserDto.builder()
                .id("111111111111111111")
                .username("updateduser")
                .displayName("Updated User")
                .email("updated@example.com")
                .avatarUrl("https://avatar.com/updated.png")
                .build();

        // When
        Mono<User> result = userRepository.save(user)
                .flatMap(savedUser -> {
                    savedUser.updateInfo(discordUser);
                    return userRepository.save(savedUser);
                });

        // Then
        StepVerifier.create(result)
                .assertNext(updatedUser -> {
                    assertThat(updatedUser.getUsername()).isEqualTo("updateduser");
                    assertThat(updatedUser.getDisplayName()).isEqualTo("Updated User");
                    assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
                    assertThat(updatedUser.getAvatarUrl()).isEqualTo("https://avatar.com/updated.png");
                    assertThat(updatedUser.getDiscordId()).isEqualTo("111111111111111111"); // 불변
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Discord ID가 중복되면 저장에 실패한다")
    void shouldFailToSaveUserWithDuplicateDiscordId() {
        // Given
        String duplicateDiscordId = "222222222222222222";

        User user1 = User.builder()
                .discordId(duplicateDiscordId)
                .username("user1")
                .displayName("User 1")
                .email("user1@example.com")
                .build();

        User user2 = User.builder()
                .discordId(duplicateDiscordId)
                .username("user2")
                .displayName("User 2")
                .email("user2@example.com")
                .build();

        // When
        Mono<User> result = userRepository.save(user1)
                .then(userRepository.save(user2));

        // Then
        StepVerifier.create(result)
                .expectError()
                .verify(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("모든 사용자를 조회할 수 있다")
    void shouldFindAllUsers() {
        // Given
        User user1 = User.builder()
                .discordId("333333333333333333")
                .username("user1")
                .displayName("User 1")
                .email("user1@example.com")
                .build();

        User user2 = User.builder()
                .discordId("444444444444444444")
                .username("user2")
                .displayName("User 2")
                .email("user2@example.com")
                .build();

        // When
        Mono<Long> userCount = userRepository.save(user1)
                .then(userRepository.save(user2))
                .then(userRepository.count());

        // Then
        StepVerifier.create(userCount)
                .assertNext(count -> assertThat(count).isGreaterThanOrEqualTo(2))
                .verifyComplete();
    }

    @Test
    @DisplayName("사용자를 삭제할 수 있다")
    void shouldDeleteUser() {
        // Given
        User user = User.builder()
                .discordId("555555555555555555")
                .username("deleteuser")
                .displayName("Delete User")
                .email("delete@example.com")
                .build();

        // When
        Mono<Boolean> result = userRepository.save(user)
                .flatMap(savedUser -> userRepository.deleteById(savedUser.getId()))
                .then(userRepository.findByDiscordId("555555555555555555"))
                .map(foundUser -> false)
                .defaultIfEmpty(true);

        // Then
        StepVerifier.create(result)
                .assertNext(deleted -> assertThat(deleted).isTrue())
                .verifyComplete();
    }
}
