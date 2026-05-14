package com.yeswater.bids.config.application;

import com.yeswater.bids.config.infrastructure.web.ApiException;
import com.yeswater.bids.config.interfaces.dto.DataSourceRequest;
import com.yeswater.bids.config.interfaces.dto.SqlModelRequest;
import com.yeswater.bids.config.interfaces.dto.ValidateResponse;
import com.yeswater.bids.config.domain.model.DataSourceConfig;
import com.yeswater.bids.config.domain.model.SqlModelConfig;
import com.yeswater.bids.config.domain.model.SqlModelStatus;
import com.yeswater.bids.config.infrastructure.persistence.ConfigRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ConfigService {
    private final ConfigRepository configRepository;
    private final SqlTemplateService sqlTemplateService;
    private final SqlSafetyValidator sqlSafetyValidator;

    public ConfigService(
            ConfigRepository configRepository,
            SqlTemplateService sqlTemplateService,
            SqlSafetyValidator sqlSafetyValidator
    ) {
        this.configRepository = configRepository;
        this.sqlTemplateService = sqlTemplateService;
        this.sqlSafetyValidator = sqlSafetyValidator;
    }

    @Transactional
    public DataSourceConfig createDataSource(DataSourceRequest request) {
        return configRepository.saveDataSource(request);
    }

    @Transactional
    public SqlModelConfig createModel(SqlModelRequest request) {
        requireDatasource(request.datasourceCode());
        validateTemplate(request.sqlTemplate());
        return configRepository.createModel(request);
    }

    @Transactional
    public SqlModelConfig updateModel(String id, SqlModelRequest request) {
        requireModel(id);
        requireDatasource(request.datasourceCode());
        validateTemplate(request.sqlTemplate());
        return configRepository.updateModel(id, request);
    }

    public SqlModelConfig getModel(String id) {
        return requireModel(id);
    }

    public ValidateResponse validateModel(String id) {
        SqlModelConfig config = requireModel(id);
        String renderedSql = sqlTemplateService.render(config.model().sqlTemplate(), Map.of());
        sqlSafetyValidator.validateReadonlySelect(renderedSql);
        return new ValidateResponse(true, "SQL 校验通过", renderedSql);
    }

    @Transactional
    public SqlModelConfig publish(String id) {
        SqlModelConfig config = requireModel(id);
        validateModel(id);
        configRepository.updateStatus(config.model().id(), SqlModelStatus.PUBLISHED);
        return requireModel(id);
    }

    @Transactional
    public SqlModelConfig offline(String id) {
        SqlModelConfig config = requireModel(id);
        configRepository.updateStatus(config.model().id(), SqlModelStatus.OFFLINE);
        return requireModel(id);
    }

    private void validateTemplate(String sqlTemplate) {
        String renderedSql = sqlTemplateService.render(sqlTemplate, Map.of());
        sqlSafetyValidator.validateReadonlySelect(renderedSql);
    }

    private void requireDatasource(String datasourceCode) {
        configRepository.findDataSource(datasourceCode)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "数据源不存在：" + datasourceCode));
    }

    private SqlModelConfig requireModel(String id) {
        return configRepository.findModelById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SQL 模型不存在：" + id));
    }
}
