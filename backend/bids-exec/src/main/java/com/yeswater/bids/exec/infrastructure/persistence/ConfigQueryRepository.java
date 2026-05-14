package com.yeswater.bids.exec.infrastructure.persistence;

import com.yeswater.bids.exec.domain.model.DataSourceConfig;
import com.yeswater.bids.exec.domain.model.ExecuteLog;
import com.yeswater.bids.exec.domain.model.FieldType;
import com.yeswater.bids.exec.domain.model.FormField;
import com.yeswater.bids.exec.domain.model.ModelPermission;
import com.yeswater.bids.exec.domain.model.ResultColumn;
import com.yeswater.bids.exec.domain.model.SqlModel;
import com.yeswater.bids.exec.domain.model.SqlModelConfig;
import com.yeswater.bids.exec.domain.model.SqlModelStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class ConfigQueryRepository {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ConfigQueryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<DataSourceConfig> findDataSource(String code) {
        return jdbcTemplate.query("""
                select id, code, name, jdbc_url, username, password, driver_class_name, max_pool_size, active
                from bids_datasource
                where code = :code
                """, new MapSqlParameterSource("code", code), datasourceMapper()).stream().findFirst();
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
