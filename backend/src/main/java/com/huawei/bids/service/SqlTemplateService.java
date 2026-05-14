package com.huawei.bids.service;

import com.huawei.bids.common.ApiException;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
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
            StringTemplateLoader loader = new StringTemplateLoader();
            String name = UUID.randomUUID().toString();
            loader.putTemplate(name, sqlTemplate);
            configuration.setTemplateLoader(loader);
            Template template = configuration.getTemplate(name);
            StringWriter writer = new StringWriter();
            template.process(parameters == null ? Map.of() : parameters, writer);
            return writer.toString();
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SQL 模板渲染失败：" + exception.getMessage());
        }
    }
}
