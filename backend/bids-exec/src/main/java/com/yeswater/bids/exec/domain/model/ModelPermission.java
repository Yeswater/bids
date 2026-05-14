package com.yeswater.bids.exec.domain.model;

public record ModelPermission(
        String id,
        String modelId,
        String username,
        String roleCode
) {
}
