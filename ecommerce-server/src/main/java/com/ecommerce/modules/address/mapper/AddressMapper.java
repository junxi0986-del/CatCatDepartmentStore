package com.ecommerce.modules.address.mapper;

import com.ecommerce.modules.address.entity.UserAddress;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface AddressMapper {

    @Select("SELECT * FROM user_address WHERE user_id = #{userId} ORDER BY is_default DESC, create_time DESC")
    List<UserAddress> selectByUserId(Long userId);

    @Select("SELECT * FROM user_address WHERE id = #{id}")
    UserAddress selectById(Long id);

    @Insert("INSERT INTO user_address (user_id, receiver, phone, province, city, area, detail_address, is_default, create_time) VALUES (#{userId}, #{receiver}, #{phone}, #{province}, #{city}, #{area}, #{detailAddress}, #{isDefault}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserAddress address);

    @Update("UPDATE user_address SET receiver = #{receiver}, phone = #{phone}, province = #{province}, city = #{city}, area = #{area}, detail_address = #{detailAddress}, is_default = #{isDefault} WHERE id = #{id}")
    int update(UserAddress address);

    @Delete("DELETE FROM user_address WHERE id = #{id}")
    int delete(Long id);

    @Update("UPDATE user_address SET is_default = 0 WHERE user_id = #{userId}")
    int updateDefaultToFalse(Long userId);
}
