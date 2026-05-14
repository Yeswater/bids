package com.huawei.bids.dto;

import com.huawei.bids.model.FieldType;

public record FormFieldResponse(
        String fieldName,
        String label,
        FieldType fieldType,
        boolean required,
        String defaultValue,
        String optionsJson,
        int sortOrder
) {
}
