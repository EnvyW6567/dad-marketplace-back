package org.envyw.dadmarketplace.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AttributeDto(
        String id,
        String display,
        String field,
        @JsonProperty("is_percentage") boolean isPercentage
) {
}
