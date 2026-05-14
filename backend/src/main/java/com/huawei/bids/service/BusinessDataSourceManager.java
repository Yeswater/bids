package com.huawei.bids.service;

import com.huawei.bids.common.ApiException;
import com.huawei.bids.model.DataSourceConfig;
import com.huawei.bids.repository.ConfigRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BusinessDataSourceManager {
    private final ConfigRepository configRepository;
    private final Map<String, HikariDataSource> cache = new ConcurrentHashMap<>();

    public BusinessDataSourceManager(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public NamedParameterJdbcTemplate jdbcTemplate(String datasourceCode) {
        HikariDataSource dataSource = cache.computeIfAbsent(datasourceCode, this::createDataSource);
        return new NamedParameterJdbcTemplate(dataSource);
    }

    private HikariDataSource createDataSource(String datasourceCode) {
        DataSourceConfig config = configRepository.findDataSource(datasourceCode)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "数据源不存在：" + datasourceCode));
        if (!config.active()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "数据源未启用：" + datasourceCode);
        }
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.jdbcUrl());
        hikariConfig.setUsername(config.username());
        hikariConfig.setPassword(config.password());
        hikariConfig.setDriverClassName(config.driverClassName());
        hikariConfig.setMaximumPoolSize(config.maxPoolSize());
        hikariConfig.setPoolName("bids-" + datasourceCode);
        return new HikariDataSource(hikariConfig);
    }
}
