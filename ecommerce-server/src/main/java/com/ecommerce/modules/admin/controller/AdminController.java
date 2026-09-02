package com.ecommerce.modules.admin.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.config.AdminTokenManager;
import com.ecommerce.modules.admin.entity.Admin;
import com.ecommerce.modules.admin.mapper.AdminMapper;
import com.ecommerce.modules.admin.mapper.AdminRolePermissionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private AdminRolePermissionMapper adminRolePermissionMapper;

    @PostMapping("/login")
    public Result login(@RequestBody Map<String, String> params) {
        String username = params.get("username");
        String password = params.get("password");

        Admin admin = adminMapper.selectByUsername(username);
        if (admin == null) {
            return Result.fail("用户名或密码错误");
        }

        if (!admin.getPassword().equals(password)) {
            return Result.fail("用户名或密码错误");
        }

        if (admin.getStatus() != 1) {
            return Result.fail("管理员账号已禁用");
        }

        String token = UUID.randomUUID().toString();
        AdminTokenManager.put(token, String.valueOf(admin.getId()));

        List<String> permissionKeys = getPermissionsByRoleKey(admin.getRoleKey());

        Map<String, Object> data = new HashMap<>();
        data.put("token", token);

        Map<String, Object> adminInfo = new HashMap<>();
        adminInfo.put("id", admin.getId());
        adminInfo.put("username", admin.getUsername());
        adminInfo.put("nickname", admin.getNickname());
        adminInfo.put("role", admin.getRole());
        adminInfo.put("roleKey", admin.getRoleKey());
        adminInfo.put("permissions", permissionKeys);
        data.put("admin", adminInfo);

        return Result.success(data);
    }

    @GetMapping("/info")
    public Result getAdminInfo(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null && AdminTokenManager.contains(token)) {
            String adminId = AdminTokenManager.getAdminId(token);
            Admin admin = adminMapper.selectById(Long.valueOf(adminId));
            if (admin != null) {
                List<String> permissionKeys = getPermissionsByRoleKey(admin.getRoleKey());
                Map<String, Object> adminInfo = new HashMap<>();
                adminInfo.put("id", admin.getId());
                adminInfo.put("username", admin.getUsername());
                adminInfo.put("nickname", admin.getNickname());
                adminInfo.put("role", admin.getRole());
                adminInfo.put("roleKey", admin.getRoleKey());
                adminInfo.put("permissions", permissionKeys);
                return Result.success(adminInfo);
            }
        }
        return Result.error(401, "未登录或登录已过期");
    }

    @PostMapping("/logout")
    public Result logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token != null) {
            AdminTokenManager.remove(token);
        }
        return Result.success();
    }

    private List<String> getPermissionsByRoleKey(String roleKey) {
        if (roleKey == null || roleKey.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<String> permissionKeys = adminRolePermissionMapper.selectPermissionKeysByRoleKey(roleKey);
        return permissionKeys != null ? permissionKeys : new ArrayList<>();
    }
}
