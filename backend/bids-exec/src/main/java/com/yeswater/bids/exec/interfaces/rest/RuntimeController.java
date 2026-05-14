package com.yeswater.bids.exec.interfaces.rest;

import com.yeswater.bids.exec.interfaces.dto.ExecuteRequest;
import com.yeswater.bids.exec.interfaces.dto.ExecuteResponse;
import com.yeswater.bids.exec.interfaces.dto.FormResponse;
import com.yeswater.bids.exec.interfaces.dto.LogResponse;
import com.yeswater.bids.exec.application.RuntimeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/runtime")
public class RuntimeController {
    private final RuntimeService runtimeService;

    public RuntimeController(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    /** 获取已发布模型的前台表单配置。 */
    @GetMapping("/models/{modelCode}/form")
    public FormResponse getForm(@PathVariable String modelCode) {
        return runtimeService.getForm(modelCode);
    }

    /** 执行已发布 SQL 模型并返回表格数据。 */
    @PostMapping("/models/{modelCode}/execute")
    public ExecuteResponse execute(@PathVariable String modelCode, @RequestBody ExecuteRequest request) {
        return runtimeService.execute(modelCode, request);
    }

    /** 查询指定执行编号的审计日志。 */
    @GetMapping("/logs/{executeId}")
    public LogResponse getLog(@PathVariable String executeId) {
        return runtimeService.getLog(executeId);
    }
}
