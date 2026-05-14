package com.yeswater.bids.config.domain.model;

public record FormField(
        String id,
        String modelId,
        String fieldName,
        String label,
        FieldType fieldType,
        boolean required,
        String defaultValue,
        String optionsJson,
        int sortOrder
) {
}
