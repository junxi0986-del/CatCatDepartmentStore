package com.ecommerce.modules.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.modules.chat.entity.Knowledge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 知识库Mapper接口
 * 提供知识库数据的数据库操作方法
 * 
 * @author ecommerce-team
 * @since 1.0.0
 */
@Mapper
public interface KnowledgeMapper extends BaseMapper<Knowledge> {
    
    /**
     * 查询所有知识，按创建时间降序排列
     * 
     * @return 知识列表
     */
    @Select("SELECT * FROM knowledge ORDER BY created_at DESC")
    List<Knowledge> selectAll();
    
    /**
     * 根据分类查询知识
     * 
     * @param category 知识分类（product/logistics/after_sale/coupon/general）
     * @return 该分类下的知识列表
     */
    @Select("SELECT * FROM knowledge WHERE category = #{category} ORDER BY created_at DESC")
    List<Knowledge> selectByCategory(@Param("category") String category);
    
    /**
     * 根据关键词搜索知识
     * 在知识内容和关键词字段中进行模糊匹配
     * 
     * @param keyword 搜索关键词
     * @return 匹配的知识列表
     */
    @Select("SELECT * FROM knowledge WHERE content LIKE CONCAT('%', #{keyword}, '%') OR keywords LIKE CONCAT('%', #{keyword}, '%') ORDER BY created_at DESC")
    List<Knowledge> searchByKeyword(@Param("keyword") String keyword);
    
    /**
     * 统计指定内容的知识数量
     * 用于检查知识是否重复
     * 
     * @param content 知识内容
     * @return 匹配的数量
     */
    @Select("SELECT COUNT(*) FROM knowledge WHERE content = #{content}")
    int countByContent(@Param("content") String content);
    
    /**
     * 根据内容精确查询知识
     * 用于检查知识是否已存在
     * 
     * @param content 知识内容
     * @return 知识对象，不存在则返回null
     */
    @Select("SELECT * FROM knowledge WHERE content = #{content} LIMIT 1")
    Knowledge selectByContent(@Param("content") String content);
}
