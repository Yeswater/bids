package com.yeswater.bids.exec.interfaces.dto;

import java.util.List;

public record FormResponse(
        String modelCode,
        String modelName,
        List<FormFieldResponse> fields,
        List<ResultColumnResponse> columns
) {
}
