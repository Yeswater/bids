package com.yeswater.bids.exec.infrastructure.datasource;

import com.yeswater.bids.exec.infrastructure.web.ApiException;
import com.yeswater.bids.exec.domain.model.DataSourceConfig;
import com.yeswater.bids.exec.infrastructure.persistence.ConfigQueryRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BusinessDataSourceManager {
    private final ConfigQueryRepository configRepository;
    private final Map<String, HikariDataSource> cache = new ConcurrentHashMap<>();

    public BusinessDataSourceManager(ConfigQueryRepository configRepository) {
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
