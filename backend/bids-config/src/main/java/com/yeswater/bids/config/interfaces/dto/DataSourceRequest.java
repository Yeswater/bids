package com.yeswater.bids.config.interfaces.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DataSourceRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String jdbcUrl,
        @NotBlank String username,
        @NotBlank String password,
        @NotBlank String driverClassName,
        @Min(1) @Max(50) int maxPoolSize,
        boolean active
) {
}
