package com.ecommerce.config;

import com.ecommerce.modules.admin.entity.Admin;
import com.ecommerce.modules.admin.mapper.AdminMapper;
import com.ecommerce.modules.admin.mapper.AdminRolePermissionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private AdminRolePermissionMapper adminRolePermissionMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || token.trim().isEmpty()) {
            sendError(response, 401, "未登录");
            return false;
        }

        String adminId = AdminTokenManager.getAdminId(token);
        if (adminId == null) {
            sendError(response, 401, "登录已过期");
            return false;
        }

        String requiredPermission = getRequiredPermission(request.getRequestURI());
        if (requiredPermission == null) {
            return true;
        }

        Admin admin = adminMapper.selectById(Long.valueOf(adminId));
        if (admin == null) {
            sendError(response, 401, "账号不存在");
            return false;
        }

        if ("super_admin".equals(admin.getRoleKey())) {
            return true;
        }

        List<String> permissions = adminRolePermissionMapper.selectPermissionKeysByRoleKey(admin.getRoleKey());
        if (permissions == null || !permissions.contains(requiredPermission)) {
            sendError(response, 403, "无操作权限");
            return false;
        }

        return true;
    }

    private String getRequiredPermission(String uri) {
        if (uri == null) return null;
        if (uri.startsWith("/api/admin/manage")) return "admin";
        if (uri.startsWith("/api/user/manage")) return "user";
        if (uri.startsWith("/api/product/manage")) return "product";
        if (uri.startsWith("/api/order/manage")) return "order";
        if (uri.startsWith("/api/coupon/manage")) return "coupon";
        if (uri.startsWith("/api/behavior/manage")) return "behavior";
        if (uri.startsWith("/api/system/manage")) return "system";
        if (uri.startsWith("/api/knowledge")) return "knowledge";
        if (uri.startsWith("/api/service/manage")) return "service";
        return null;
    }

    private void sendError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", status);
        result.put("message", message);
        result.put("data", null);
        response.getWriter().write(new ObjectMapper().writeValueAsString(result));
    }
}
