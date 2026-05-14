package com.yeswater.bids.config.interfaces.rest;

import com.yeswater.bids.config.interfaces.dto.DataSourceRequest;
import com.yeswater.bids.config.interfaces.dto.SqlModelRequest;
import com.yeswater.bids.config.interfaces.dto.ValidateResponse;
import com.yeswater.bids.config.domain.model.DataSourceConfig;
import com.yeswater.bids.config.domain.model.SqlModelConfig;
import com.yeswater.bids.config.application.ConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    /** 创建业务数据源配置。 */
    @PostMapping("/datasources")
    public DataSourceConfig createDataSource(@Valid @RequestBody DataSourceRequest request) {
        return configService.createDataSource(request);
    }

    /** 创建 SQL 模型草稿。 */
    @PostMapping("/models")
    public SqlModelConfig createModel(@Valid @RequestBody SqlModelRequest request) {
        return configService.createModel(request);
    }

    /** 更新 SQL 模型并回到草稿态。 */
    @PutMapping("/models/{id}")
    public SqlModelConfig updateModel(@PathVariable String id, @Valid @RequestBody SqlModelRequest request) {
        return configService.updateModel(id, request);
    }

    /** 查询 SQL 模型详情。 */
    @GetMapping("/models/{id}")
    public SqlModelConfig getModel(@PathVariable String id) {
        return configService.getModel(id);
    }

    /** 校验 SQL 模板是否为安全只读查询。 */
    @PostMapping("/models/{id}/validate")
    public ValidateResponse validateModel(@PathVariable String id) {
        return configService.validateModel(id);
    }

    /** 发布 SQL 模型供运行态执行。 */
    @PostMapping("/models/{id}/publish")
    public SqlModelConfig publish(@PathVariable String id) {
        return configService.publish(id);
    }

    /** 下线 SQL 模型并禁止运行态执行。 */
    @PostMapping("/models/{id}/offline")
    public SqlModelConfig offline(@PathVariable String id) {
        return configService.offline(id);
    }
}
