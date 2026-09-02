package com.ecommerce.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder.build();
        
        // 配置UTF-8编码
        List<StringHttpMessageConverter> converters = restTemplate.getMessageConverters().stream()
                .filter(c -> c instanceof StringHttpMessageConverter)
                .map(c -> (StringHttpMessageConverter) c)
                .toList();
        
        for (StringHttpMessageConverter converter : converters) {
            converter.setDefaultCharset(StandardCharsets.UTF_8);
        }
        
        // 如果没有StringHttpMessageConverter，添加一个
        if (converters.isEmpty()) {
            restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        }
        
        return restTemplate;
    }
}
