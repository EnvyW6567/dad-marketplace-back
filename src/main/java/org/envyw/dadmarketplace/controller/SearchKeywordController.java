package org.envyw.dadmarketplace.controller;

import lombok.RequiredArgsConstructor;
import org.envyw.dadmarketplace.application.dto.response.SearchKeywordResDto;
import org.envyw.dadmarketplace.application.dto.searchKey.AttributeDto;
import org.envyw.dadmarketplace.application.dto.searchKey.EquipmentDto;
import org.envyw.dadmarketplace.application.dto.searchKey.RarityDto;
import org.envyw.dadmarketplace.application.service.SearchKeywordService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/search-keyword")
@RequiredArgsConstructor
public class SearchKeywordController {

    private final SearchKeywordService searchKeywordService;

    @GetMapping("/attributes")
    public Mono<SearchKeywordResDto<AttributeDto>> getAttributeKeyword() {
        return searchKeywordService.getAttributes();
    }

    @GetMapping("/rarities")
    public Mono<SearchKeywordResDto<RarityDto>> getRarityKeyword() {
        return searchKeywordService.getRarities();
    }

    @GetMapping("/equipments")
    public Mono<SearchKeywordResDto<EquipmentDto>> getEquipmentKeyword() {
        return searchKeywordService.getEquipments();
    }
}
