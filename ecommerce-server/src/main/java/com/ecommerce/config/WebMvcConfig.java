package com.ecommerce.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AdminAuthInterceptor adminAuthInterceptor;

    @Bean
    public StringHttpMessageConverter stringHttpMessageConverter() {
        return new StringHttpMessageConverter(StandardCharsets.UTF_8);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/manage/**", "/api/user/manage/**", "/api/product/manage/**",
                        "/api/order/manage/**", "/api/coupon/manage/**", "/api/behavior/manage/**",
                        "/api/system/manage/**", "/api/knowledge/**", "/api/service/manage/**")
                .excludePathPatterns("/api/admin/login", "/api/admin/info");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/");
        
        registry.addResourceHandler("/Bannerimg/**")
                .addResourceLocations("classpath:/static/Bannerimg/");
        
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("classpath:/static/avatars/");
        
        registry.addResourceHandler("/userimages/**")
                .addResourceLocations("classpath:/static/userimages/");
        
        registry.addResourceHandler("/couponimg/**")
                .addResourceLocations("classpath:/static/couponimg/");
        
        registry.addResourceHandler("/chat_images/**")
                .addResourceLocations("file:D:/Desktop/ecommerce-platform/ecommerce-server/src/main/resources/static/chat_images/");
        
        registry.addResourceHandler("/indbackground.png")
                .addResourceLocations("classpath:/static/");
        
        registry.addResourceHandler("/indeedbackground.png")
                .addResourceLocations("classpath:/static/");
    }
}
