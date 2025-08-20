package org.envyw.dadmarketplace.application.dto.response;

import java.util.List;

public record SearchKeywordResDto<T>(
        List<T> body
) {
}
