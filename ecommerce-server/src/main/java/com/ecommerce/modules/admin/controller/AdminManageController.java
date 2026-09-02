package com.ecommerce.modules.admin.controller;

import com.ecommerce.common.result.Result;
import com.ecommerce.modules.admin.entity.Admin;
import com.ecommerce.modules.admin.entity.AdminPermission;
import com.ecommerce.modules.admin.entity.AdminRole;
import com.ecommerce.modules.admin.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/manage")
public class AdminManageController {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private AdminRoleMapper adminRoleMapper;

    @Autowired
    private AdminPermissionMapper adminPermissionMapper;

    @Autowired
    private AdminRolePermissionMapper adminRolePermissionMapper;

    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String username,
                       @RequestParam(required = false) String roleKey,
                       @RequestParam(required = false) Integer status) {
        List<Admin> admins = adminMapper.selectByCondition(username, roleKey, status);
        for (Admin admin : admins) {
            admin.setPassword(null);
        }
        return Result.success(admins);
    }

    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            return Result.fail("管理员不存在");
        }
        admin.setPassword(null);
        return Result.success(admin);
    }

    @PostMapping("/add")
    public Result add(@RequestBody Admin admin) {
        if (admin.getUsername() == null || admin.getUsername().trim().isEmpty()) {
            return Result.fail("用户名不能为空");
        }
        if (admin.getPassword() == null || admin.getPassword().trim().isEmpty()) {
            return Result.fail("密码不能为空");
        }
        if (adminMapper.countByUsername(admin.getUsername()) > 0) {
            return Result.fail("用户名已存在");
        }
        if (admin.getRoleKey() == null || admin.getRoleKey().trim().isEmpty()) {
            admin.setRoleKey("store_admin");
        }
        if (admin.getStatus() == null) {
            admin.setStatus(1);
        }
        admin.setRole(0);
        adminMapper.insert(admin);
        return Result.success();
    }

    @PutMapping("/update")
    public Result update(@RequestBody Admin admin) {
        if (admin.getId() == null) {
            return Result.fail("管理员ID不能为空");
        }
        Admin existing = adminMapper.selectById(admin.getId());
        if (existing == null) {
            return Result.fail("管理员不存在");
        }
        if ("super_admin".equals(existing.getRoleKey()) && !"super_admin".equals(admin.getRoleKey())) {
            long superAdminCount = adminMapper.selectByCondition(null, "super_admin", 1).size();
            if (superAdminCount <= 1) {
                return Result.fail("系统至少需要保留一个启用的超级管理员");
            }
        }
        if (admin.getUsername() != null && !admin.getUsername().equals(existing.getUsername())) {
            if (adminMapper.countByUsernameExcludeId(admin.getUsername(), admin.getId()) > 0) {
                return Result.fail("用户名已存在");
            }
        }
        adminMapper.update(admin);
        return Result.success();
    }

    @PutMapping("/resetPassword")
    public Result resetPassword(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        String password = (String) params.get("password");
        if (password == null || password.trim().isEmpty()) {
            return Result.fail("新密码不能为空");
        }
        adminMapper.updatePassword(id, password);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result delete(@PathVariable Long id) {
        Admin existing = adminMapper.selectById(id);
        if (existing == null) {
            return Result.fail("管理员不存在");
        }
        if ("super_admin".equals(existing.getRoleKey())) {
            long superAdminCount = adminMapper.selectByCondition(null, "super_admin", 1).size();
            if (superAdminCount <= 1) {
                return Result.fail("不能删除最后一个超级管理员");
            }
        }
        adminMapper.deleteById(id);
        return Result.success();
    }

    @PutMapping("/status")
    public Result updateStatus(@RequestBody Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Integer status = Integer.valueOf(params.get("status").toString());
        Admin existing = adminMapper.selectById(id);
        if (existing == null) {
            return Result.fail("管理员不存在");
        }
        if ("super_admin".equals(existing.getRoleKey()) && status != 1) {
            long superAdminCount = adminMapper.selectByCondition(null, "super_admin", 1).size();
            if (superAdminCount <= 1) {
                return Result.fail("不能禁用最后一个超级管理员");
            }
        }
        Admin admin = new Admin();
        admin.setId(id);
        admin.setStatus(status);
        admin.setRoleKey(existing.getRoleKey());
        admin.setUsername(existing.getUsername());
        admin.setNickname(existing.getNickname());
        adminMapper.update(admin);
        return Result.success();
    }

    @GetMapping("/roles")
    public Result listRoles() {
        List<AdminRole> roles = adminRoleMapper.selectAll();
        return Result.success(roles);
    }

    @GetMapping("/permissions")
    public Result listPermissions() {
        List<AdminPermission> permissions = adminPermissionMapper.selectAll();
        return Result.success(permissions);
    }

    @GetMapping("/role/permissions/{roleKey}")
    public Result getRolePermissions(@PathVariable String roleKey) {
        List<String> permissionKeys = adminRolePermissionMapper.selectPermissionKeysByRoleKey(roleKey);
        return Result.success(permissionKeys);
    }

    @PutMapping("/role/permissions")
    public Result updateRolePermissions(@RequestBody Map<String, Object> params) {
        String roleKey = (String) params.get("roleKey");
        @SuppressWarnings("unchecked")
        List<String> permissionKeys = (List<String>) params.get("permissionKeys");
        if (roleKey == null || roleKey.trim().isEmpty()) {
            return Result.fail("角色标识不能为空");
        }
        if ("super_admin".equals(roleKey)) {
            return Result.fail("超级管理员权限不可修改");
        }
        adminRolePermissionMapper.deleteByRoleKey(roleKey);
        if (permissionKeys != null && !permissionKeys.isEmpty()) {
            adminRolePermissionMapper.batchInsert(roleKey, permissionKeys);
        }
        return Result.success();
    }
}
