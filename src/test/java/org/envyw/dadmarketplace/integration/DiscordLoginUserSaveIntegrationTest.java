package org.envyw.dadmarketplace.integration;

import io.r2dbc.spi.ConnectionFactory;
import org.envyw.dadmarketplace.config.R2dbcConfig;
import org.envyw.dadmarketplace.entity.User;
import org.envyw.dadmarketplace.repository.UserRepository;
import org.envyw.dadmarketplace.security.CustomOAuth2LoginSuccessHandler;
import org.envyw.dadmarketplace.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(R2dbcConfig.class)
@Testcontainers
@DisplayName("Discord 로그인 사용자 정보 저장 통합 테스트")
class DiscordLoginUserSaveIntegrationTest {

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

    @Autowired
    private UserService userService;

    @Autowired
    private CustomOAuth2LoginSuccessHandler successHandler;

    @BeforeAll
    static void initSchema(@Autowired ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("schema/schema.sql"));

        initializer.setDatabasePopulator(populator);
        initializer.afterPropertiesSet();
    }

    @Test
    @DisplayName("새로운 Discord 사용자 로그인 시 사용자 정보가 데이터베이스에 저장되어야 한다")
    void shouldSaveNewDiscordUserToDatabase() {
        // Given - 새로운 Discord 사용자 정보
        Map<String, Object> attributes = createDiscordUserAttributes(
                "123456789012345678",
                "testuser",
                "abcdef123456",
                "test@example.com",
                "Test User"
        );

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );

        OAuth2AuthenticationToken authenticationToken = new OAuth2AuthenticationToken(
                oauth2User,
                oauth2User.getAuthorities(),
                "discord"
        );

        // When - 사용자 정보 추출 및 저장
        Mono<User> result = Mono.fromCallable(() -> successHandler.extractDiscordUserInfo(oauth2User))
                .flatMap(discordUserDto -> userService.saveOrUpdateUser(discordUserDto));

        // Then - 사용자가 데이터베이스에 저장되었는지 확인
        StepVerifier.create(result)
                .assertNext(savedUser -> {
                    assertThat(savedUser.getId()).isNotNull();
                    assertThat(savedUser.getDiscordId()).isEqualTo("123456789012345678");
                    assertThat(savedUser.getUsername()).isEqualTo("testuser");
                    assertThat(savedUser.getDisplayName()).isEqualTo("Test User");
                    assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
                    assertThat(savedUser.getAvatarUrl()).contains("123456789012345678/abcdef123456");
                    assertThat(savedUser.getCreatedAt()).isNotNull();
                    assertThat(savedUser.getUpdatedAt()).isNotNull();
                })
                .verifyComplete();

        // 데이터베이스에서 직접 조회하여 확인
        StepVerifier.create(userRepository.findByDiscordId("123456789012345678"))
                .assertNext(foundUser -> {
                    assertThat(foundUser.getDiscordId()).isEqualTo("123456789012345678");
                    assertThat(foundUser.getUsername()).isEqualTo("testuser");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("기존 Discord 사용자 로그인 시 사용자 정보가 업데이트되어야 한다")
    void shouldUpdateExistingDiscordUserInDatabase() {
        // Given - 기존 사용자 저장
        String discordId = "987654321098765432";
        User existingUser = User.builder()
                .discordId(discordId)
                .username("oldusername")
                .displayName("Old Display Name")
                .email("old@example.com")
                .avatarUrl("https://old-avatar.com/old.png")
                .build();

        // 기존 사용자를 먼저 저장
        Mono<User> saveExistingUser = userRepository.save(existingUser);

        // 업데이트된 Discord 사용자 정보
        Map<String, Object> updatedAttributes = createDiscordUserAttributes(
                discordId,
                "newusername",
                "newavatar123",
                "new@example.com",
                "New Display Name"
        );

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                updatedAttributes,
                "id"
        );

        // When - 기존 사용자 정보 업데이트
        Mono<User> result = saveExistingUser
                .then(Mono.fromCallable(() -> successHandler.extractDiscordUserInfo(oauth2User)))
                .flatMap(discordUserDto -> userService.saveOrUpdateUser(discordUserDto));

        // Then - 사용자 정보가 업데이트되었는지 확인
        StepVerifier.create(result)
                .assertNext(updatedUser -> {
                    assertThat(updatedUser.getDiscordId()).isEqualTo(discordId);
                    assertThat(updatedUser.getUsername()).isEqualTo("newusername");
                    assertThat(updatedUser.getDisplayName()).isEqualTo("New Display Name");
                    assertThat(updatedUser.getEmail()).isEqualTo("new@example.com");
                    assertThat(updatedUser.getAvatarUrl()).contains("newavatar123");
                })
                .verifyComplete();

        // 데이터베이스에서 직접 조회하여 업데이트 확인
        StepVerifier.create(userRepository.findByDiscordId(discordId))
                .assertNext(foundUser -> {
                    assertThat(foundUser.getUsername()).isEqualTo("newusername");
                    assertThat(foundUser.getDisplayName()).isEqualTo("New Display Name");
                    assertThat(foundUser.getEmail()).isEqualTo("new@example.com");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("아바타가 없는 Discord 사용자도 정상적으로 저장되어야 한다")
    void shouldSaveDiscordUserWithoutAvatar() {
        // Given - 아바타가 없는 Discord 사용자
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", "111111111111111111");
        attributes.put("username", "noavataruser");
        attributes.put("avatar", null); // 아바타 없음
        attributes.put("email", "noavatar@example.com");
        attributes.put("global_name", "No Avatar User");

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );

        // When - 사용자 정보 저장
        Mono<User> result = Mono.fromCallable(() -> successHandler.extractDiscordUserInfo(oauth2User))
                .flatMap(discordUserDto -> userService.saveOrUpdateUser(discordUserDto));

        // Then - 기본 아바타 URL로 저장되었는지 확인
        StepVerifier.create(result)
                .assertNext(savedUser -> {
                    assertThat(savedUser.getDiscordId()).isEqualTo("111111111111111111");
                    assertThat(savedUser.getUsername()).isEqualTo("noavataruser");
                    assertThat(savedUser.getAvatarUrl()).isEqualTo("https://dafault-avatar-url.png");
                    assertThat(savedUser.getEmail()).isEqualTo("noavatar@example.com");
                    assertThat(savedUser.getDisplayName()).isEqualTo("No Avatar User");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("이메일이 없는 Discord 사용자도 정상적으로 저장되어야 한다")
    void shouldSaveDiscordUserWithoutEmail() {
        // Given - 이메일이 없는 Discord 사용자
        Map<String, Object> attributes = createDiscordUserAttributes(
                "222222222222222222",
                "noemailuser",
                "avatar123",
                null, // 이메일 없음
                "No Email User"
        );

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );

        // When - 사용자 정보 저장
        Mono<User> result = Mono.fromCallable(() -> successHandler.extractDiscordUserInfo(oauth2User))
                .flatMap(discordUserDto -> userService.saveOrUpdateUser(discordUserDto));

        // Then - 이메일 없이도 정상 저장되었는지 확인
        StepVerifier.create(result)
                .assertNext(savedUser -> {
                    assertThat(savedUser.getDiscordId()).isEqualTo("222222222222222222");
                    assertThat(savedUser.getUsername()).isEqualTo("noemailuser");
                    assertThat(savedUser.getEmail()).isNull();
                    assertThat(savedUser.getDisplayName()).isEqualTo("No Email User");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("동시에 여러 사용자가 로그인해도 정상적으로 처리되어야 한다")
    void shouldHandleConcurrentUserLogins() {
        // Given - 여러 사용자 정보
        String[] discordIds = {
                "333333333333333333",
                "444444444444444444",
                "555555555555555555"
        };

        // When - 동시에 여러 사용자 저장
        Mono<Long> result = Flux.fromIterable(List.of(discordIds))
                .flatMap(discordId -> {
                    Map<String, Object> attributes = createDiscordUserAttributes(
                            discordId,
                            "user" + discordId.substring(0, 6),
                            "avatar" + discordId.substring(0, 6),
                            "user" + discordId.substring(0, 6) + "@example.com",
                            "User " + discordId.substring(0, 6)
                    );

                    OAuth2User oauth2User = new DefaultOAuth2User(
                            Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                            attributes,
                            "id"
                    );

                    return Mono.fromCallable(() -> successHandler.extractDiscordUserInfo(oauth2User))
                            .flatMap(discordUserDto -> userService.saveOrUpdateUser(discordUserDto));
                })
                .count();

        // Then - 모든 사용자가 저장되었는지 확인
        StepVerifier.create(result)
                .assertNext(count -> assertThat(count).isEqualTo(3))
                .verifyComplete();

        // 각 사용자가 데이터베이스에 저장되었는지 확인
        for (String discordId : discordIds) {
            StepVerifier.create(userRepository.findByDiscordId(discordId))
                    .assertNext(user -> assertThat(user.getDiscordId()).isEqualTo(discordId))
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("잘못된 Discord 사용자 정보에 대해 적절한 예외가 발생해야 한다")
    void shouldThrowExceptionForInvalidDiscordUserInfo() {
        // Given - 필수 정보가 없는 Discord 사용자
        Map<String, Object> invalidAttributes = new HashMap<>();
        invalidAttributes.put("id", "1231231212"); // 필수 정보 누락
        invalidAttributes.put("username", null);

        OAuth2User oauth2User = new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                invalidAttributes,
                "id"
        );

        // When & Then - 예외가 발생해야 함
        StepVerifier.create(
                        Mono.fromCallable(() -> successHandler.extractDiscordUserInfo(oauth2User))
                                .flatMap(discordUserDto -> userService.saveOrUpdateUser(discordUserDto))
                )
                .expectError(NullPointerException.class)
                .verify(Duration.ofSeconds(5));
    }

    private Map<String, Object> createDiscordUserAttributes(String id, String username,
                                                            String avatar, String email, String globalName) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", id);
        attributes.put("username", username);
        attributes.put("avatar", avatar);
        attributes.put("email", email);
        attributes.put("global_name", globalName);
        return attributes;
    }
}
