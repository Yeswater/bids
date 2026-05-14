package com.yeswater.bids.exec.interfaces.dto;

public record ResultColumnResponse(
        String columnName,
        String label,
        boolean visible,
        String maskType,
        int sortOrder
) {
}
