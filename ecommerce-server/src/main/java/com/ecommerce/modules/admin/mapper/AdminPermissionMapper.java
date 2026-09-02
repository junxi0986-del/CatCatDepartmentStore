package com.ecommerce.modules.admin.mapper;

import com.ecommerce.modules.admin.entity.AdminPermission;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminPermissionMapper {

    @Select("SELECT * FROM admin_permission ORDER BY sort_order")
    List<AdminPermission> selectAll();

    @Select("SELECT p.* FROM admin_permission p INNER JOIN admin_role_permission rp ON p.permission_key = rp.permission_key WHERE rp.role_key = #{roleKey} ORDER BY p.sort_order")
    List<AdminPermission> selectByRoleKey(String roleKey);

    @Select("SELECT * FROM admin_permission WHERE permission_key = #{permissionKey}")
    AdminPermission selectByPermissionKey(String permissionKey);
}
