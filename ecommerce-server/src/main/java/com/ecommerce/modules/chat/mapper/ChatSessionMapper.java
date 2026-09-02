package com.ecommerce.modules.chat.mapper;

import com.ecommerce.modules.chat.entity.ChatSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatSessionMapper {
    @Select("SELECT * FROM chat_session WHERE id = #{id}")
    ChatSession selectById(Long id);

    @Select("SELECT * FROM chat_session WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<ChatSession> selectByUserId(Long userId);

    @Select("SELECT * FROM chat_session WHERE order_id = #{orderId} ORDER BY created_at DESC LIMIT 1")
    ChatSession selectByOrderId(Long orderId);

    @Select("SELECT * FROM chat_session WHERE status IN (0, 1) ORDER BY updated_at DESC")
    List<ChatSession> selectActiveSessions();

    @Insert("INSERT INTO chat_session(user_id, order_id, status) VALUES(#{userId}, #{orderId}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatSession session);

    @Update("UPDATE chat_session SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    int updateStatus(Long id, Integer status);
}
