package com.yeswater.bids.config.interfaces.dto;

public record ModelPermissionRequest(
        String username,
        String roleCode
) {
}
