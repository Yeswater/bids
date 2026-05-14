package com.huawei.bids.repository;

import com.huawei.bids.dto.DataSourceRequest;
import com.huawei.bids.dto.FormFieldRequest;
import com.huawei.bids.dto.ModelPermissionRequest;
import com.huawei.bids.dto.ResultColumnRequest;
import com.huawei.bids.dto.SqlModelRequest;
import com.huawei.bids.model.DataSourceConfig;
import com.huawei.bids.model.ExecuteLog;
import com.huawei.bids.model.FieldType;
import com.huawei.bids.model.FormField;
import com.huawei.bids.model.ModelPermission;
import com.huawei.bids.model.ResultColumn;
import com.huawei.bids.model.SqlModel;
import com.huawei.bids.model.SqlModelConfig;
import com.huawei.bids.model.SqlModelStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ConfigRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ConfigRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public DataSourceConfig saveDataSource(DataSourceRequest request) {
        String id = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into bids_datasource
                (id, code, name, jdbc_url, username, password, driver_class_name, max_pool_size, active)
                values
                (:id, :code, :name, :jdbcUrl, :username, :password, :driverClassName, :maxPoolSize, :active)
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("code", request.code())
                .addValue("name", request.name())
                .addValue("jdbcUrl", request.jdbcUrl())
                .addValue("username", request.username())
                .addValue("password", request.password())
                .addValue("driverClassName", request.driverClassName())
                .addValue("maxPoolSize", request.maxPoolSize())
                .addValue("active", request.active()));
        return findDataSource(request.code()).orElseThrow();
    }

    public Optional<DataSourceConfig> findDataSource(String code) {
        return jdbcTemplate.query("""
                select id, code, name, jdbc_url, username, password, driver_class_name, max_pool_size, active
                from bids_datasource
                where code = :code
                """, new MapSqlParameterSource("code", code), datasourceMapper()).stream().findFirst();
    }

    public SqlModelConfig createModel(SqlModelRequest request) {
        String modelId = UUID.randomUUID().toString();
        jdbcTemplate.update("""
                insert into bids_sql_model
                (id, code, name, datasource_code, sql_template, max_rows, status)
                values
                (:id, :code, :name, :datasourceCode, :sqlTemplate, :maxRows, :status)
                """, modelParams(modelId, request, SqlModelStatus.DRAFT));
        insertChildren(modelId, request);
        return findModelById(modelId).orElseThrow();
    }

    public SqlModelConfig updateModel(String id, SqlModelRequest request) {
        jdbcTemplate.update("""
                update bids_sql_model
                set code = :code,
                    name = :name,
                    datasource_code = :datasourceCode,
                    sql_template = :sqlTemplate,
                    max_rows = :maxRows,
                    status = :status,
                    updated_at = current_timestamp
                where id = :id
                """, modelParams(id, request, SqlModelStatus.DRAFT));
        deleteChildren(id);
        insertChildren(id, request);
        return findModelById(id).orElseThrow();
    }

    public void updateStatus(String id, SqlModelStatus status) {
        jdbcTemplate.update("""
                update bids_sql_model
                set status = :status, updated_at = current_timestamp
                where id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("status", status.name()));
    }

    public Optional<SqlModelConfig> findModelById(String id) {
        return findModel("""
                select id, code, name, datasource_code, sql_template, max_rows, status
                from bids_sql_model
                where id = :id
                """, new MapSqlParameterSource("id", id));
    }

    public Optional<SqlModelConfig> findModelByCode(String code) {
        return findModel("""
                select id, code, name, datasource_code, sql_template, max_rows, status
                from bids_sql_model
                where code = :code
                """, new MapSqlParameterSource("code", code));
    }

    public void saveExecuteLog(ExecuteLog log) {
        jdbcTemplate.update("""
                insert into bids_execute_log
                (id, execute_id, model_code, username, final_sql, parameters_json, success, error_message, duration_ms, row_count)
                values
                (:id, :executeId, :modelCode, :username, :finalSql, :parametersJson, :success, :errorMessage, :durationMs, :rowCount)
                """, new MapSqlParameterSource()
                .addValue("id", log.id())
                .addValue("executeId", log.executeId())
                .addValue("modelCode", log.modelCode())
                .addValue("username", log.username())
                .addValue("finalSql", log.finalSql())
                .addValue("parametersJson", log.parametersJson())
                .addValue("success", log.success())
                .addValue("errorMessage", log.errorMessage())
                .addValue("durationMs", log.durationMs())
                .addValue("rowCount", log.rowCount()));
    }

    public Optional<ExecuteLog> findLog(String executeId) {
        return jdbcTemplate.query("""
                select id, execute_id, model_code, username, final_sql, parameters_json,
                       success, error_message, duration_ms, row_count, created_at
                from bids_execute_log
                where execute_id = :executeId
                """, new MapSqlParameterSource("executeId", executeId), logMapper()).stream().findFirst();
    }

    private Optional<SqlModelConfig> findModel(String sql, MapSqlParameterSource params) {
        Optional<SqlModel> model = jdbcTemplate.query(sql, params, modelMapper()).stream().findFirst();
        return model.map(value -> new SqlModelConfig(
                value,
                findFields(value.id()),
                findColumns(value.id()),
                findPermissions(value.id())
        ));
    }

    private List<FormField> findFields(String modelId) {
        return jdbcTemplate.query("""
                select id, model_id, field_name, label, field_type, required, default_value, options_json, sort_order
                from bids_form_field
                where model_id = :modelId
                order by sort_order asc, id asc
                """, new MapSqlParameterSource("modelId", modelId), fieldMapper());
    }

    private List<ResultColumn> findColumns(String modelId) {
        return jdbcTemplate.query("""
                select id, model_id, column_name, label, visible, mask_type, sort_order
                from bids_result_column
                where model_id = :modelId
                order by sort_order asc, id asc
                """, new MapSqlParameterSource("modelId", modelId), columnMapper());
    }

    private List<ModelPermission> findPermissions(String modelId) {
        return jdbcTemplate.query("""
                select id, model_id, username, role_code
                from bids_model_permission
                where model_id = :modelId
                """, new MapSqlParameterSource("modelId", modelId), permissionMapper());
    }

    private void insertChildren(String modelId, SqlModelRequest request) {
        for (FormFieldRequest field : emptyIfNull(request.fields())) {
            jdbcTemplate.update("""
                    insert into bids_form_field
                    (id, model_id, field_name, label, field_type, required, default_value, options_json, sort_order)
                    values
                    (:id, :modelId, :fieldName, :label, :fieldType, :required, :defaultValue, :optionsJson, :sortOrder)
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID().toString())
                    .addValue("modelId", modelId)
                    .addValue("fieldName", field.fieldName())
                    .addValue("label", field.label())
                    .addValue("fieldType", field.fieldType().name())
                    .addValue("required", field.required())
                    .addValue("defaultValue", field.defaultValue())
                    .addValue("optionsJson", field.optionsJson())
                    .addValue("sortOrder", field.sortOrder()));
        }
        for (ResultColumnRequest column : emptyIfNull(request.columns())) {
            jdbcTemplate.update("""
                    insert into bids_result_column
                    (id, model_id, column_name, label, visible, mask_type, sort_order)
                    values
                    (:id, :modelId, :columnName, :label, :visible, :maskType, :sortOrder)
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID().toString())
                    .addValue("modelId", modelId)
                    .addValue("columnName", column.columnName())
                    .addValue("label", column.label())
                    .addValue("visible", column.visible())
                    .addValue("maskType", column.maskType())
                    .addValue("sortOrder", column.sortOrder()));
        }
        for (ModelPermissionRequest permission : emptyIfNull(request.permissions())) {
            jdbcTemplate.update("""
                    insert into bids_model_permission
                    (id, model_id, username, role_code)
                    values
                    (:id, :modelId, :username, :roleCode)
                    """, new MapSqlParameterSource()
                    .addValue("id", UUID.randomUUID().toString())
                    .addValue("modelId", modelId)
                    .addValue("username", permission.username())
                    .addValue("roleCode", permission.roleCode()));
        }
    }

    private void deleteChildren(String modelId) {
        MapSqlParameterSource params = new MapSqlParameterSource("modelId", modelId);
        jdbcTemplate.update("delete from bids_form_field where model_id = :modelId", params);
        jdbcTemplate.update("delete from bids_result_column where model_id = :modelId", params);
        jdbcTemplate.update("delete from bids_model_permission where model_id = :modelId", params);
    }

    private MapSqlParameterSource modelParams(String id, SqlModelRequest request, SqlModelStatus status) {
        return new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("code", request.code())
                .addValue("name", request.name())
                .addValue("datasourceCode", request.datasourceCode())
                .addValue("sqlTemplate", request.sqlTemplate())
                .addValue("maxRows", request.maxRows())
                .addValue("status", status.name());
    }

    private <T> List<T> emptyIfNull(List<T> values) {
        return values == null ? List.of() : values;
    }

    private RowMapper<DataSourceConfig> datasourceMapper() {
        return (rs, rowNum) -> new DataSourceConfig(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("jdbc_url"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("driver_class_name"),
                rs.getInt("max_pool_size"),
                rs.getBoolean("active")
        );
    }

    private RowMapper<SqlModel> modelMapper() {
        return (rs, rowNum) -> new SqlModel(
                rs.getString("id"),
                rs.getString("code"),
                rs.getString("name"),
                rs.getString("datasource_code"),
                rs.getString("sql_template"),
                rs.getInt("max_rows"),
                SqlModelStatus.valueOf(rs.getString("status"))
        );
    }

    private RowMapper<FormField> fieldMapper() {
        return (rs, rowNum) -> new FormField(
                rs.getString("id"),
                rs.getString("model_id"),
                rs.getString("field_name"),
                rs.getString("label"),
                FieldType.valueOf(rs.getString("field_type")),
                rs.getBoolean("required"),
                rs.getString("default_value"),
                rs.getString("options_json"),
                rs.getInt("sort_order")
        );
    }

    private RowMapper<ResultColumn> columnMapper() {
        return (rs, rowNum) -> new ResultColumn(
                rs.getString("id"),
                rs.getString("model_id"),
                rs.getString("column_name"),
                rs.getString("label"),
                rs.getBoolean("visible"),
                rs.getString("mask_type"),
                rs.getInt("sort_order")
        );
    }

    private RowMapper<ModelPermission> permissionMapper() {
        return (rs, rowNum) -> new ModelPermission(
                rs.getString("id"),
                rs.getString("model_id"),
                rs.getString("username"),
                rs.getString("role_code")
        );
    }

    private RowMapper<ExecuteLog> logMapper() {
        return (rs, rowNum) -> new ExecuteLog(
                rs.getString("id"),
                rs.getString("execute_id"),
                rs.getString("model_code"),
                rs.getString("username"),
                rs.getString("final_sql"),
                rs.getString("parameters_json"),
                rs.getBoolean("success"),
                rs.getString("error_message"),
                rs.getLong("duration_ms"),
                rs.getInt("row_count"),
                toInstant(rs.getTimestamp("created_at"))
        );
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
