package com.huawei.bids.model;

public record ModelPermission(
        String id,
        String modelId,
        String username,
        String roleCode
) {
}
