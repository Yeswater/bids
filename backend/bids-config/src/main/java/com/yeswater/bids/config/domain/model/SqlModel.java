package com.yeswater.bids.config.domain.model;

public record SqlModel(
        String id,
        String code,
        String name,
        String datasourceCode,
        String sqlTemplate,
        int maxRows,
        SqlModelStatus status
) {
}
