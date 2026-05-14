package com.yeswater.bids.config.interfaces.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record SqlModelRequest(
        @NotBlank String code,
        @NotBlank String name,
        @NotBlank String datasourceCode,
        @NotBlank String sqlTemplate,
        @Min(1) @Max(10000) int maxRows,
        @Valid List<FormFieldRequest> fields,
        @Valid List<ResultColumnRequest> columns,
        @Valid List<ModelPermissionRequest> permissions
) {
}
