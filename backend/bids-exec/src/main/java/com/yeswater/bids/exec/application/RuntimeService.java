package com.yeswater.bids.exec.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yeswater.bids.exec.infrastructure.audit.AuditSearchPublisher;
import com.yeswater.bids.exec.infrastructure.web.ApiException;
import com.yeswater.bids.exec.interfaces.dto.ExecuteRequest;
import com.yeswater.bids.exec.interfaces.dto.ExecuteResponse;
import com.yeswater.bids.exec.interfaces.dto.FormFieldResponse;
import com.yeswater.bids.exec.interfaces.dto.FormResponse;
import com.yeswater.bids.exec.interfaces.dto.LogResponse;
import com.yeswater.bids.exec.interfaces.dto.ResultColumnResponse;
import com.yeswater.bids.exec.domain.model.ExecuteLog;
import com.yeswater.bids.exec.domain.model.FieldType;
import com.yeswater.bids.exec.domain.model.FormField;
import com.yeswater.bids.exec.domain.model.ModelPermission;
import com.yeswater.bids.exec.domain.model.ResultColumn;
import com.yeswater.bids.exec.domain.model.SqlModelConfig;
import com.yeswater.bids.exec.domain.model.SqlModelStatus;
import com.yeswater.bids.exec.infrastructure.datasource.BusinessDataSourceManager;
import com.yeswater.bids.exec.infrastructure.persistence.ConfigQueryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RuntimeService {
    private final ConfigQueryRepository configRepository;
    private final SqlTemplateService sqlTemplateService;
    private final SqlSafetyValidator sqlSafetyValidator;
    private final SqlDisplayFormatter sqlDisplayFormatter;
    private final BusinessDataSourceManager dataSourceManager;
    private final AuditSearchPublisher auditSearchPublisher;
    private final ObjectMapper objectMapper;

    public RuntimeService(
            ConfigQueryRepository configRepository,
            SqlTemplateService sqlTemplateService,
            SqlSafetyValidator sqlSafetyValidator,
            SqlDisplayFormatter sqlDisplayFormatter,
            BusinessDataSourceManager dataSourceManager,
            AuditSearchPublisher auditSearchPublisher,
            ObjectMapper objectMapper
    ) {
        this.configRepository = configRepository;
        this.sqlTemplateService = sqlTemplateService;
        this.sqlSafetyValidator = sqlSafetyValidator;
        this.sqlDisplayFormatter = sqlDisplayFormatter;
        this.dataSourceManager = dataSourceManager;
        this.auditSearchPublisher = auditSearchPublisher;
        this.objectMapper = objectMapper;
    }

    public FormResponse getForm(String modelCode) {
        SqlModelConfig config = requirePublishedModel(modelCode);
        ensurePermission(config);
        return new FormResponse(
                config.model().code(),
                config.model().name(),
                config.fields().stream().map(this::toFieldResponse).toList(),
                config.columns().stream().map(this::toColumnResponse).toList()
        );
    }

    public ExecuteResponse execute(String modelCode, ExecuteRequest request) {
        String executeId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        String finalSql = "";
        Map<String, Object> parameters = request == null || request.parameters() == null ? Map.of() : request.parameters();
        try {
            SqlModelConfig config = requirePublishedModel(modelCode);
            ensurePermission(config);
            Map<String, Object> bindParameters = validateAndConvertParameters(config.fields(), parameters);
            String renderedSql = sqlTemplateService.render(config.model().sqlTemplate(), bindParameters);
            sqlSafetyValidator.validateReadonlySelect(renderedSql);
            finalSql = sqlDisplayFormatter.toDisplaySql(renderedSql, bindParameters);

            NamedParameterJdbcTemplate jdbcTemplate = dataSourceManager.jdbcTemplate(config.model().datasourceCode());
            Map<String, Object> queryParameters = new HashMap<>(bindParameters);
            queryParameters.put("__bids_limit", config.model().maxRows() + 1);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(wrapLimit(renderedSql), queryParameters);
            List<Map<String, Object>> visibleRows = rows.stream()
                    .limit(config.model().maxRows())
                    .map(row -> filterAndMaskRow(row, config.columns()))
                    .toList();
            long durationMs = System.currentTimeMillis() - start;
            saveAudit(executeId, modelCode, currentUsername(), finalSql, bindParameters, true, null, durationMs, visibleRows.size());
            return new ExecuteResponse(
                    executeId,
                    finalSql,
                    config.columns().stream().filter(ResultColumn::visible).map(this::toColumnResponse).toList(),
                    visibleRows,
                    visibleRows.size(),
                    durationMs
            );
        } catch (Exception exception) {
            long durationMs = System.currentTimeMillis() - start;
            saveAudit(executeId, modelCode, currentUsername(), finalSql, parameters, false, exception.getMessage(), durationMs, 0);
            throw exception;
        }
    }

    public LogResponse getLog(String executeId) {
        ExecuteLog log = configRepository.findLog(executeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "执行日志不存在：" + executeId));
        return new LogResponse(
                log.executeId(),
                log.modelCode(),
                log.username(),
                log.finalSql(),
                log.parametersJson(),
                log.success(),
                log.errorMessage(),
                log.durationMs(),
                log.rowCount(),
                log.createdAt()
        );
    }

    private SqlModelConfig requirePublishedModel(String modelCode) {
        SqlModelConfig config = configRepository.findModelByCode(modelCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SQL 模型不存在：" + modelCode));
        if (config.model().status() != SqlModelStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SQL 模型未发布：" + modelCode);
        }
        return config;
    }

    private void ensurePermission(SqlModelConfig config) {
        List<ModelPermission> permissions = config.permissions();
        if (permissions.isEmpty()) {
            return;
        }
        String username = currentUsername();
        Set<String> roles = currentRoles();
        boolean allowed = permissions.stream().anyMatch(permission ->
                username.equals(permission.username())
                        || roles.contains(permission.roleCode())
                        || roles.contains("ROLE_" + permission.roleCode())
                        || "ALL".equalsIgnoreCase(permission.roleCode()));
        if (!allowed) {
            throw new ApiException(HttpStatus.FORBIDDEN, "没有模型执行权限：" + config.model().code());
        }
    }

    private Map<String, Object> validateAndConvertParameters(List<FormField> fields, Map<String, Object> requestParameters) {
        Map<String, FormField> fieldMap = fields.stream()
                .collect(Collectors.toMap(FormField::fieldName, Function.identity()));
        for (String name : requestParameters.keySet()) {
            if (!fieldMap.containsKey(name)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "参数不在白名单内：" + name);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        for (FormField field : fields) {
            Object value = requestParameters.get(field.fieldName());
            if (isBlank(value) && field.defaultValue() != null && !field.defaultValue().isBlank()) {
                value = field.defaultValue();
            }
            if (field.required() && isBlank(value)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "必填参数不能为空：" + field.fieldName());
            }
            if (!isBlank(value)) {
                result.put(field.fieldName(), convertValue(field.fieldType(), value, field.fieldName()));
            }
        }
        return result;
    }

    private Object convertValue(FieldType fieldType, Object value, String fieldName) {
        try {
            return switch (fieldType) {
                case TEXT, SELECT -> String.valueOf(value);
                case NUMBER -> new BigDecimal(String.valueOf(value));
                case BOOLEAN -> value instanceof Boolean booleanValue ? booleanValue : Boolean.parseBoolean(String.valueOf(value));
                case DATE -> LocalDate.parse(String.valueOf(value));
                case DATETIME -> LocalDateTime.parse(String.valueOf(value));
            };
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "参数类型不合法：" + fieldName);
        }
    }

    private Map<String, Object> filterAndMaskRow(Map<String, Object> row, List<ResultColumn> columns) {
        if (columns.isEmpty()) {
            return row;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (ResultColumn column : columns) {
            if (!column.visible()) {
                continue;
            }
            Object value = row.get(column.columnName());
            result.put(column.columnName(), mask(value, column.maskType()));
        }
        return result;
    }

    private Object mask(Object value, String maskType) {
        if (value == null || maskType == null || maskType.isBlank()) {
            return value;
        }
        String text = String.valueOf(value);
        return switch (maskType.toUpperCase()) {
            case "FULL" -> "******";
            case "PHONE" -> text.length() <= 7 ? "******" : text.substring(0, 3) + "****" + text.substring(text.length() - 4);
            case "EMAIL" -> maskEmail(text);
            default -> value;
        };
    }

    private String maskEmail(String text) {
        int at = text.indexOf('@');
        if (at <= 1) {
            return "******";
        }
        return text.charAt(0) + "****" + text.substring(at);
    }

    private String wrapLimit(String sql) {
        return "select * from (" + sql + ") bids_result limit :__bids_limit";
    }

    private void saveAudit(
            String executeId,
            String modelCode,
            String username,
            String finalSql,
            Map<String, Object> parameters,
            boolean success,
            String errorMessage,
            long durationMs,
            int rowCount
    ) {
        ExecuteLog log = new ExecuteLog(
                UUID.randomUUID().toString(),
                executeId,
                modelCode,
                username,
                finalSql == null ? "" : finalSql,
                toJson(parameters),
                success,
                errorMessage,
                durationMs,
                rowCount,
                null
        );
        configRepository.saveExecuteLog(log);
        try {
            auditSearchPublisher.publish(log);
        } catch (Exception ignored) {
            // Elasticsearch 审计是旁路能力，不能影响主查询链路。
        }
    }

    private String toJson(Map<String, Object> parameters) {
        try {
            return objectMapper.writeValueAsString(parameters);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private FormFieldResponse toFieldResponse(FormField field) {
        return new FormFieldResponse(
                field.fieldName(),
                field.label(),
                field.fieldType(),
                field.required(),
                field.defaultValue(),
                field.optionsJson(),
                field.sortOrder()
        );
    }

    private ResultColumnResponse toColumnResponse(ResultColumn column) {
        return new ResultColumnResponse(
                column.columnName(),
                column.label(),
                column.visible(),
                column.maskType(),
                column.sortOrder()
        );
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }

    private Set<String> currentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Set.of();
        }
        return authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).isBlank();
    }
}
