package com.yeswater.bids.exec.domain.model;

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
