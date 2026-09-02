package com.ecommerce.modules.admin.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminRolePermissionMapper {

    @Select("SELECT permission_key FROM admin_role_permission WHERE role_key = #{roleKey}")
    List<String> selectPermissionKeysByRoleKey(String roleKey);

    @Delete("DELETE FROM admin_role_permission WHERE role_key = #{roleKey}")
    int deleteByRoleKey(String roleKey);

    @Insert("<script>" +
            "INSERT INTO admin_role_permission (role_key, permission_key) VALUES " +
            "<foreach collection='permissionKeys' item='pk' separator=','>" +
            "(#{roleKey}, #{pk})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("roleKey") String roleKey, @Param("permissionKeys") List<String> permissionKeys);
}
