package com.huawei.bids.model;

public record ResultColumn(
        String id,
        String modelId,
        String columnName,
        String label,
        boolean visible,
        String maskType,
        int sortOrder
) {
}
