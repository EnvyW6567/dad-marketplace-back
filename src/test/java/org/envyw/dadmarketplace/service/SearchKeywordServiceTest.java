package org.envyw.dadmarketplace.service;

import org.envyw.dadmarketplace.dto.AttributeDto;
import org.envyw.dadmarketplace.dto.EquipmentDto;
import org.envyw.dadmarketplace.dto.RarityDto;
import org.envyw.dadmarketplace.dto.response.SearchKeywordResDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchKeywordService 테스트")
class SearchKeywordServiceTest {

    private SearchKeywordService searchKeywordService;

    @BeforeEach
    void setUp() {
        searchKeywordService = new SearchKeywordService();
    }

    @Test
    @DisplayName("아이템 속성 목록을 반환할 수 있어야 한다")
    void shouldReturnAttributes() {
        // When
        Mono<SearchKeywordResDto<AttributeDto>> result = searchKeywordService.getAttributes();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.body()).isNotEmpty();

                    // 첫 번째 속성 검증
                    AttributeDto firstAttribute = response.body().getFirst();
                    assertThat(firstAttribute.id()).isEqualTo("ActionSpeed");
                    assertThat(firstAttribute.display()).isEqualTo("Action Speed");
                    assertThat(firstAttribute.field()).isEqualTo("action_speed");
                    assertThat(firstAttribute.isPercentage()).isTrue();

                    // 총 개수 검증 (attributes.json 파일 기준)
                    assertThat(response.body()).hasSize(54);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("희귀도 목록을 반환할 수 있어야 한다")
    void shouldReturnRarities() {
        // When
        Mono<SearchKeywordResDto<RarityDto>> result = searchKeywordService.getRarities();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.body()).isNotEmpty();

                    // 첫 번째 희귀도 검증
                    RarityDto firstRarity = response.body().getFirst();
                    assertThat(firstRarity.id()).isEqualTo(1);
                    assertThat(firstRarity.name()).isEqualTo("Poor");

                    // 총 개수 검증 (rarities.json 파일 기준)
                    assertThat(response.body()).hasSize(8);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("장비 목록을 반환할 수 있어야 한다")
    void shouldReturnEquipments() {
        // When
        Mono<SearchKeywordResDto<EquipmentDto>> result = searchKeywordService.getEquipments();

        // Then
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.body()).isNotEmpty();

                    // 아머와 무기가 모두 포함되어 있는지 확인
                    boolean hasArmor = response.body().stream()
                            .anyMatch(equipment -> "Arcane Hood".equals(equipment.name()));
                    boolean hasWeapon = response.body().stream()
                            .anyMatch(equipment -> "Arming Sword".equals(equipment.name()));

                    assertThat(hasArmor).isTrue();
                    assertThat(hasWeapon).isTrue();

                    // 전체 장비 개수 확인 (대략적인 개수)
                    assertThat(response.body().size()).isGreaterThan(200);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("캐싱이 정상적으로 동작해야 한다")
    void shouldCacheJsonFiles() {
        // Given
        searchKeywordService.clearCache();
        assertThat(searchKeywordService.isCached("attributes.json")).isFalse();

        // When - 첫 번째 호출
        StepVerifier.create(searchKeywordService.getAttributes())
                .assertNext(response -> assertThat(response.body()).isNotEmpty())
                .verifyComplete();

        // Then - 캐시에 저장되었는지 확인
        assertThat(searchKeywordService.isCached("attributes.json")).isTrue();

        // When - 두 번째 호출 (캐시에서 로드)
        StepVerifier.create(searchKeywordService.getAttributes())
                .assertNext(response -> assertThat(response.body()).isNotEmpty())
                .verifyComplete();
    }

    @Test
    @DisplayName("캐시 클리어가 정상적으로 동작해야 한다")
    void shouldClearCacheCorrectly() {
        // Given - 데이터를 로드하여 캐시에 저장
        StepVerifier.create(searchKeywordService.getAttributes())
                .assertNext(response -> assertThat(response.body()).isNotEmpty())
                .verifyComplete();

        assertThat(searchKeywordService.isCached("attributes.json")).isTrue();

        // When
        searchKeywordService.clearCache();

        // Then
        assertThat(searchKeywordService.isCached("attributes.json")).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 파일 로딩 시 예외를 발생시켜야 한다")
    void shouldThrowExceptionWhenFileNotExists() {
        // Given - 존재하지 않는 파일명으로 테스트하기 위해 리플렉션 사용
        SearchKeywordService testService = new SearchKeywordService() {
            @Override
            public Mono<SearchKeywordResDto<AttributeDto>> getAttributes() {
                return loadJsonFile("non-existent-file.json")
                        .map(this::mapToAttributes);
            }

            // private 메서드를 테스트하기 위해 public으로 오버라이드
            public Mono<Map<String, Object>> loadJsonFile(String fileName) {
                return super.loadJsonFile(fileName);
            }

            private SearchKeywordResDto<AttributeDto> mapToAttributes(Map<String, Object> jsonMap) {
                return new SearchKeywordResDto<>(new ArrayList<>());
            }
        };

        // When & Then
        StepVerifier.create(testService.getAttributes())
                .expectError(RuntimeException.class)
                .verify();
    }
}
