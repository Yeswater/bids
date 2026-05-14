package com.huawei.bids.dto;

import java.util.List;

public record FormResponse(
        String modelCode,
        String modelName,
        List<FormFieldResponse> fields,
        List<ResultColumnResponse> columns
) {
}
