package com.yeswater.bids.config.interfaces.dto;

import com.yeswater.bids.config.domain.model.FieldType;
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
