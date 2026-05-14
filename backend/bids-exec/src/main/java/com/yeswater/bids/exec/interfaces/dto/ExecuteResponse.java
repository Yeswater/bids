package com.yeswater.bids.exec.interfaces.dto;

import java.util.List;
import java.util.Map;

public record ExecuteResponse(
        String executeId,
        String finalSql,
        List<ResultColumnResponse> columns,
        List<Map<String, Object>> rows,
        int rowCount,
        long durationMs
) {
}
