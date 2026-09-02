package com.ecommerce.modules.admin.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.config.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/system")
public class SystemManageController {

    @Autowired
    private ConfigService configService;

    @GetMapping("/config")
    public Result getSystemConfig() {
        Map<String, String> configMap = configService.getAllConfigs();
        // 转换为前端需要的格式
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("mallName", configMap.getOrDefault("site_name", "电商平台"));
        resultMap.put("deepseekApiKey", configMap.getOrDefault("deepseek_api_key", ""));
        resultMap.put("mallDescription", "这是一个电商平台");
        resultMap.put("orderAutoCancel", configMap.getOrDefault("order_auto_cancel", "30"));
        return Result.success(resultMap);
    }

    @PostMapping("/config")
    public Result updateSystemConfig(@RequestBody Map<String, Object> config) {
        // 转换为数据库配置格式
        Map<String, String> configMap = new HashMap<>();
        if (config.containsKey("mallName")) {
            configMap.put("site_name", config.get("mallName").toString());
        }
        if (config.containsKey("deepseekApiKey")) {
            configMap.put("deepseek_api_key", config.get("deepseekApiKey").toString());
        }
        if (config.containsKey("orderAutoCancel")) {
            configMap.put("order_auto_cancel", config.get("orderAutoCancel").toString());
        }
        
        configService.updateConfigs(configMap);
        System.out.println("更新系统配置：" + configMap);
        return Result.success("更新成功");
    }

}
