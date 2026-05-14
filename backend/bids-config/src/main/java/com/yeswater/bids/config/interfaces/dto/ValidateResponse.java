package com.yeswater.bids.config.interfaces.dto;

public record ValidateResponse(
        boolean valid,
        String message,
        String renderedSql
) {
}
