package org.envyw.dadmarketplace.dto.response;

import java.util.List;

public record SearchKeywordResDto<T>(
        List<T> body
) {
}
