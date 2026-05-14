package com.huawei.bids.model;

import java.time.Instant;

public record ExecuteLog(
        String id,
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
