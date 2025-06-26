package org.envyw.dadmarketplace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.envyw.dadmarketplace.dto.AttributeDto;
import org.envyw.dadmarketplace.dto.EquipmentDto;
import org.envyw.dadmarketplace.dto.RarityDto;
import org.envyw.dadmarketplace.dto.response.SearchKeywordResDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SearchKeywordService {

    private final ObjectMapper objectMapper;
    private final Map<String, Object> fileCache = new ConcurrentHashMap<>();

    public SearchKeywordService() {
        this.objectMapper = new ObjectMapper();
    }

    public Mono<SearchKeywordResDto<AttributeDto>> getAttributes() {
        return loadJsonFile("attributes.json")
                .map(this::mapBodyFromJsonMap)
                .map(this::mapToAttributes);
    }

    public Mono<SearchKeywordResDto<RarityDto>> getRarities() {
        return loadJsonFile("rarities.json")
                .map(this::mapBodyFromJsonMap)
                .map(this::mapToRarities);
    }

    public Mono<SearchKeywordResDto<EquipmentDto>> getEquipments() {
        return loadJsonFile("equipments.json")
                .map(this::mapBodyFromJsonMap)
                .map(this::mapToEquipments);
    }

    @SuppressWarnings("unchecked")
    @Cacheable(value = "jsonFiles", key = "#fileName")
    public Mono<Map<String, Object>> loadJsonFile(String fileName) {
        return Mono.fromCallable(() -> {
            if (fileCache.containsKey(fileName)) {

                return (Map<String, Object>) fileCache.get(fileName);
            }

            try {
                InputStream inputStream = new ClassPathResource("json/" + fileName).getInputStream();
                Map<String, Object> jsonMap = objectMapper.readValue(inputStream, Map.class);

                fileCache.put(fileName, jsonMap);

                return jsonMap;

            } catch (IOException e) {
                log.error("JSON 파일 로딩 실패: {}", fileName, e);
                throw new RuntimeException("JSON 파일을 로딩하는데 실패했습니다: " + fileName, e);
            }
        });
    }

    private SearchKeywordResDto<AttributeDto> mapToAttributes(List<Map<String, Object>> body) {
        List<AttributeDto> attributes = new ArrayList<>();

        for (Map<String, Object> item : body) {
            AttributeDto attribute = new AttributeDto(
                    (String) item.get("id"),
                    (String) item.get("display"),
                    (String) item.get("field"),
                    (Boolean) item.get("is_percentage")
            );
            attributes.add(attribute);
        }

        return new SearchKeywordResDto<>(attributes);
    }

    private SearchKeywordResDto<RarityDto> mapToRarities(List<Map<String, Object>> body) {
        List<RarityDto> rarities = new ArrayList<>();

        for (Map<String, Object> item : body) {
            RarityDto rarity = new RarityDto(
                    (Integer) item.get("id"),
                    (String) item.get("name")
            );
            rarities.add(rarity);
        }

        return new SearchKeywordResDto<>(rarities);
    }

    private SearchKeywordResDto<EquipmentDto> mapToEquipments(List<Map<String, Object>> body) {
        List<EquipmentDto> equipments = new ArrayList<>();

        for (Map<String, Object> item : body) {
            EquipmentDto equipment = new EquipmentDto(
                    (String) item.get("name"),
                    (String) item.get("archetype")
            );
            equipments.add(equipment);
        }

        return new SearchKeywordResDto<>(equipments);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapBodyFromJsonMap(Map<String, Object> jsonMap) {
        return (List<Map<String, Object>>) jsonMap.get("body");
    }

    public void clearCache() {
        fileCache.clear();
        log.info("파일 캐시 클리어 완료");
    }

    protected boolean isCached(String fileName) {
        return fileCache.containsKey(fileName);
    }
}
