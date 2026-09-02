package com.ecommerce.modules.config.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.modules.config.entity.Config;
import com.ecommerce.modules.config.mapper.ConfigMapper;
import com.ecommerce.modules.config.service.ConfigService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ConfigServiceImpl extends ServiceImpl<ConfigMapper, Config> implements ConfigService {

    @Override
    public Map<String, String> getAllConfigs() {
        List<Config> configs = list();
        Map<String, String> configMap = new HashMap<>();
        for (Config config : configs) {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        }
        return configMap;
    }

    @Override
    public String getConfigValue(String key) {
        Config config = lambdaQuery().eq(Config::getConfigKey, key).one();
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public boolean updateConfigs(Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            Config config = lambdaQuery().eq(Config::getConfigKey, key).one();
            if (config != null) {
                config.setConfigValue(value);
                config.setUpdateTime(new Date());
                updateById(config);
            } else {
                // 如果配置不存在，创建新配置
                Config newConfig = new Config();
                newConfig.setConfigKey(key);
                newConfig.setConfigValue(value);
                newConfig.setUpdateTime(new Date());
                save(newConfig);
            }
        }
        return true;
    }

}
