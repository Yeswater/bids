package com.yeswater.bids.exec.application;

import com.yeswater.bids.exec.infrastructure.web.ApiException;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.io.StringReader;
import java.util.Map;
import java.util.UUID;

@Service
public class SqlTemplateService {
    private final Configuration configuration;

    public SqlTemplateService() {
        this.configuration = new Configuration(Configuration.VERSION_2_3_34);
        this.configuration.setDefaultEncoding("UTF-8");
        this.configuration.setNumberFormat("computer");
        this.configuration.setBooleanFormat("true,false");
    }

    public String render(String sqlTemplate, Map<String, Object> parameters) {
        try {
            rejectDirectInterpolation(sqlTemplate);
            Template template = new Template("sql-" + UUID.randomUUID(), new StringReader(sqlTemplate), configuration);
            StringWriter writer = new StringWriter();
            template.process(parameters == null ? Map.of() : parameters, writer);
            return writer.toString();
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SQL 模板渲染失败：" + exception.getMessage());
        }
    }

    private void rejectDirectInterpolation(String sqlTemplate) {
        if (sqlTemplate.contains("${") || sqlTemplate.contains("#{")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SQL 模板禁止直接插值用户参数，请使用命名参数绑定");
        }
    }
}
