package com.yeswater.bids.config.domain.model;

public record ModelPermission(
        String id,
        String modelId,
        String username,
        String roleCode
) {
}
