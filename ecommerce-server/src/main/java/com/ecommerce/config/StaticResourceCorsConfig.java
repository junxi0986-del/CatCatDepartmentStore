package com.ecommerce.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class StaticResourceCorsConfig {

    @Bean
    public FilterRegistrationBean<Filter> staticResourceCorsFilter() {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) 
                    throws IOException, ServletException {
                
                HttpServletResponse response = (HttpServletResponse) res;
                HttpServletRequest request = (HttpServletRequest) req;
                
                String requestURI = request.getRequestURI();
                
                boolean isStaticResource = requestURI.contains("/images/") || 
                        requestURI.contains("/avatars/") || 
                        requestURI.contains("/userimages/") || 
                        requestURI.contains("/couponimg/");
                
                if (isStaticResource) {
                    response.setHeader("Access-Control-Allow-Origin", "*");
                    response.setHeader("Cross-Origin-Resource-Policy", "cross-origin");
                    response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, OPTIONS");
                    response.setHeader("Access-Control-Allow-Headers", "*");
                    response.setHeader("Access-Control-Max-Age", "3600");
                    
                    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        return;
                    }
                }
                
                chain.doFilter(req, res);
            }
        });
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
