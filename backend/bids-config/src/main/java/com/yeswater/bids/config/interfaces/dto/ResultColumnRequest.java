package com.yeswater.bids.config.interfaces.dto;

import jakarta.validation.constraints.NotBlank;

public record ResultColumnRequest(
        @NotBlank String columnName,
        @NotBlank String label,
        boolean visible,
        String maskType,
        int sortOrder
) {
}
