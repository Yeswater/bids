package com.huawei.bids.dto;

public record ResultColumnResponse(
        String columnName,
        String label,
        boolean visible,
        String maskType,
        int sortOrder
) {
}
