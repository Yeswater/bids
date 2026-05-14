package com.huawei.bids.dto;

public record ValidateResponse(
        boolean valid,
        String message,
        String renderedSql
) {
}
