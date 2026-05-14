package com.huawei.bids.dto;

import com.huawei.bids.model.FieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record FormFieldRequest(
        @NotBlank String fieldName,
        @NotBlank String label,
        @NotNull FieldType fieldType,
        boolean required,
        String defaultValue,
        String optionsJson,
        int sortOrder
) {
}
