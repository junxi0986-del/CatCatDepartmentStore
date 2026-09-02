package com.ecommerce.modules.user.mapper;

import com.ecommerce.modules.user.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM user WHERE id = #{id}")
    User selectById(Long id);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM user WHERE phone = #{phone}")
    User selectByPhone(String phone);

    @Insert("INSERT INTO user (username, password, phone, email, avatar, create_time, update_time) VALUES (#{username}, #{password}, #{phone}, #{email}, #{avatar}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE user SET username = #{username}, password = #{password}, phone = #{phone}, email = #{email}, avatar = #{avatar}, status = #{status}, update_time = NOW() WHERE id = #{id}")
    int update(User user);

    @Update("UPDATE user SET username = #{username}, email = #{email}, avatar = #{avatar}, update_time = NOW() WHERE id = #{id}")
    int updateUserInfo(Long id, String username, String email, String avatar);

    @Update("UPDATE user SET password = #{password}, update_time = NOW() WHERE id = #{id}")
    int updatePassword(Long id, String password);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int delete(Long id);

    @Update("UPDATE user SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(Long id, Integer status);

    @Select("SELECT * FROM user")
    List<User> selectAll();

    @Select("SELECT * FROM user WHERE username LIKE CONCAT('%', #{keyword}, '%') OR phone LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%')")
    List<User> selectByKeyword(String keyword);

    @Select("<script>" +
            "SELECT * FROM user WHERE 1=1 " +
            "<if test='username != null and username != \"\"'>" +
            "AND username LIKE CONCAT('%', #{username}, '%') " +
            "</if>" +
            "<if test='phone != null and phone != \"\"'>" +
            "AND phone LIKE CONCAT('%', #{phone}, '%') " +
            "</if>" +
            "<if test='email != null and email != \"\"'>" +
            "AND email LIKE CONCAT('%', #{email}, '%') " +
            "</if>" +
            "<if test='status != null'>" +
            "AND status = #{status} " +
            "</if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<User> searchUsers(@Param("username") String username,
                           @Param("phone") String phone,
                           @Param("email") String email,
                           @Param("status") Integer status);
}
