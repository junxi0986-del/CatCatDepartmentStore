package com.ecommerce.modules.admin.mapper;

import com.ecommerce.modules.admin.entity.Admin;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AdminMapper {

    @Select("SELECT * FROM admin WHERE username = #{username}")
    Admin selectByUsername(String username);

    @Select("SELECT * FROM admin WHERE id = #{id}")
    Admin selectById(Long id);

    @Select("SELECT * FROM admin ORDER BY create_time DESC")
    List<Admin> selectAll();

    @Select("<script>" +
            "SELECT * FROM admin WHERE 1=1 " +
            "<if test='username != null and username != \"\"'>" +
            " AND username LIKE CONCAT('%', #{username}, '%')" +
            "</if>" +
            "<if test='roleKey != null and roleKey != \"\"'>" +
            " AND role_key = #{roleKey}" +
            "</if>" +
            "<if test='status != null'>" +
            " AND status = #{status}" +
            "</if>" +
            " ORDER BY create_time DESC" +
            "</script>")
    List<Admin> selectByCondition(@Param("username") String username, @Param("roleKey") String roleKey, @Param("status") Integer status);

    @Insert("INSERT INTO admin (username, password, nickname, role, role_key, status) VALUES (#{username}, #{password}, #{nickname}, #{role}, #{roleKey}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Admin admin);

    @Update("UPDATE admin SET username = #{username}, nickname = #{nickname}, role_key = #{roleKey}, status = #{status} WHERE id = #{id}")
    int update(Admin admin);

    @Update("UPDATE admin SET password = #{password} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Delete("DELETE FROM admin WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM admin WHERE username = #{username}")
    int countByUsername(@Param("username") String username);

    @Select("SELECT COUNT(*) FROM admin WHERE username = #{username} AND id != #{excludeId}")
    int countByUsernameExcludeId(@Param("username") String username, @Param("excludeId") Long excludeId);
}
