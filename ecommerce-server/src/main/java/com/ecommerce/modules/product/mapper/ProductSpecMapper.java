package com.ecommerce.modules.product.mapper;

import com.ecommerce.modules.product.entity.ProductSpec;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ProductSpecMapper {
    @Select("SELECT * FROM product_spec WHERE product_id = #{productId} ORDER BY sort ASC")
    List<ProductSpec> selectByProductId(Long productId);

    @Select("SELECT * FROM product_spec WHERE id = #{id}")
    ProductSpec selectById(Long id);

    @Insert("INSERT INTO product_spec (product_id, spec_name, spec_value, price, stock, image, sort) VALUES (#{productId}, #{specName}, #{specValue}, #{price}, #{stock}, #{image}, #{sort})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ProductSpec spec);

    @Update("UPDATE product_spec SET spec_name=#{specName}, spec_value=#{specValue}, price=#{price}, stock=#{stock}, image=#{image}, sort=#{sort} WHERE id=#{id}")
    void update(ProductSpec spec);

    @Delete("DELETE FROM product_spec WHERE id = #{id}")
    void deleteById(Long id);

    @Delete("DELETE FROM product_spec WHERE product_id = #{productId}")
    void deleteByProductId(Long productId);

    @Select("SELECT DISTINCT spec_name FROM product_spec WHERE product_id = #{productId} ORDER BY sort ASC")
    List<String> selectSpecNamesByProductId(Long productId);

    @Update("UPDATE product_spec SET stock = stock - #{quantity} WHERE id = #{id} AND stock >= #{quantity}")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
