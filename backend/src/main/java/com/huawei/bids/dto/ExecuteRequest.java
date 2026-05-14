package com.huawei.bids.dto;

import java.util.Map;

public record ExecuteRequest(
        Map<String, Object> parameters
) {
}
