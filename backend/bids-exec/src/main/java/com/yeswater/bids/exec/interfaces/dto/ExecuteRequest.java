package com.yeswater.bids.exec.interfaces.dto;

import java.util.Map;

public record ExecuteRequest(
        Map<String, Object> parameters
) {
}
