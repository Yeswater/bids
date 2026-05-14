package com.yeswater.bids.exec.infrastructure.audit;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.Instant;

@Document(indexName = "bids_execute_log")
public record ExecuteLogDocument(
        @Id String executeId,
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
