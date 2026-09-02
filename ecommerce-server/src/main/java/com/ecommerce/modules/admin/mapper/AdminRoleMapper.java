package com.ecommerce.modules.admin.mapper;

import com.ecommerce.modules.admin.entity.AdminRole;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminRoleMapper {

    @Select("SELECT * FROM admin_role ORDER BY id")
    List<AdminRole> selectAll();

    @Select("SELECT * FROM admin_role WHERE role_key = #{roleKey}")
    AdminRole selectByRoleKey(String roleKey);

    @Insert("INSERT INTO admin_role (role_key, role_name, description) VALUES (#{roleKey}, #{roleName}, #{description})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AdminRole adminRole);

    @Update("UPDATE admin_role SET role_name = #{roleName}, description = #{description} WHERE role_key = #{roleKey}")
    int updateByRoleKey(AdminRole adminRole);

    @Delete("DELETE FROM admin_role WHERE role_key = #{roleKey}")
    int deleteByRoleKey(String roleKey);
}
