package com.ecommerce.modules.config.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.modules.config.entity.Config;

import java.util.Map;

public interface ConfigService extends IService<Config> {

    /**
     * 获取所有配置
     * @return 配置Map
     */
    Map<String, String> getAllConfigs();

    /**
     * 根据键名获取配置值
     * @param key 配置键名
     * @return 配置值
     */
    String getConfigValue(String key);

    /**
     * 更新配置
     * @param configMap 配置Map
     * @return 是否更新成功
     */
    boolean updateConfigs(Map<String, String> configMap);

}
