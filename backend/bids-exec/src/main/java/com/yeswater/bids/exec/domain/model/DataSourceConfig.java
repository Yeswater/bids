package com.yeswater.bids.exec.domain.model;

public record DataSourceConfig(
        String id,
        String code,
        String name,
        String jdbcUrl,
        String username,
        String password,
        String driverClassName,
        int maxPoolSize,
        boolean active
) {
}
