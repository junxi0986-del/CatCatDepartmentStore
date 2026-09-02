package com.ecommerce.modules.product.mapper;

import com.ecommerce.modules.product.entity.Product;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ProductMapper {
    @Select("<script>SELECT * FROM product WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>")
    List<Product> selectBatchIds(@Param("ids") List<Long> ids);

    @Select("SELECT * FROM product")
    List<Product> selectAll();

    @Select("SELECT * FROM product WHERE id = #{id}")
    Product selectById(Long id);

    @Select("SELECT * FROM product WHERE name LIKE CONCAT('%', #{keyword}, '%') OR pic LIKE CONCAT('%', #{keyword}, '%')")
    List<Product> selectByKeyword(String keyword);

    @Select("<script>" +
            "SELECT * FROM product WHERE 1=1 " +
            "<if test='name != null and name != \"\"'>" +
            "AND name LIKE CONCAT('%', #{name}, '%') " +
            "</if>" +
            "<if test='categoryId != null'>" +
            "AND category_id = #{categoryId} " +
            "</if>" +
            "<if test='minPrice != null'>" +
            "AND price >= #{minPrice} " +
            "</if>" +
            "<if test='maxPrice != null'>" +
            "AND price &lt;= #{maxPrice} " +
            "</if>" +
            "<if test='minStock != null'>" +
            "AND stock >= #{minStock} " +
            "</if>" +
            "<if test='maxStock != null'>" +
            "AND stock &lt;= #{maxStock} " +
            "</if>" +
            "<if test='status != null'>" +
            "AND status = #{status} " +
            "</if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<Product> searchProducts(@Param("name") String name,
                                 @Param("categoryId") Integer categoryId,
                                 @Param("minPrice") Double minPrice,
                                 @Param("maxPrice") Double maxPrice,
                                 @Param("minStock") Integer minStock,
                                 @Param("maxStock") Integer maxStock,
                                 @Param("status") Integer status);

    @Insert("INSERT INTO product (name, category_id, price, market_price, stock, pic, images, detail, status, sales, create_time, update_time) VALUES (#{name}, #{categoryId}, #{price}, #{marketPrice}, #{stock}, #{pic}, #{images}, #{detail}, #{status}, #{sales}, NOW(), NOW())")
    void insert(Product product);

    @Update("UPDATE product SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    @Update("UPDATE product SET name = #{name}, category_id = #{categoryId}, price = #{price}, market_price = #{marketPrice}, stock = #{stock}, pic = #{pic}, images = #{images}, detail = #{detail}, status = #{status}, sales = #{sales}, update_time = NOW() WHERE id = #{id}")
    void update(Product product);

    @Delete("DELETE FROM product WHERE id = #{id}")
    void delete(Long id);

    @Update("UPDATE product SET stock = stock - #{quantity}, sales = sales + #{quantity}, update_time = NOW() WHERE id = #{id} AND stock >= #{quantity}")
    int decreaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    @Update("UPDATE product SET stock = stock + #{quantity}, update_time = NOW() WHERE id = #{id}")
    void increaseStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
