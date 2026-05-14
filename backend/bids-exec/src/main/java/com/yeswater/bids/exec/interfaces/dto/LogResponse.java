package com.yeswater.bids.exec.interfaces.dto;

import java.time.Instant;

public record LogResponse(
        String executeId,
        String modelCode,
        String username,
        String finalSql,
        String parametersJson,
        boolean success,
        String errorMessage,
        long durationMs,
        int rowCount,
        Instant createdAt
) {
}
