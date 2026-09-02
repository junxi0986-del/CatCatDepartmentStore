package com.ecommerce.modules.chat.mapper;

import com.ecommerce.modules.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface ChatMapper {

    @Insert("INSERT INTO `chat_message` (user_id, user_name, admin_id, admin_name, content, type, create_time, session_id, is_read, message_type, image_url) VALUES (#{userId}, #{userName}, #{adminId}, #{adminName}, #{content}, #{type}, NOW(), #{sessionId}, 0, #{messageType}, #{imageUrl})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage chatMessage);

    @Select("SELECT user_id FROM `chat_message` WHERE admin_id = #{adminId} GROUP BY user_id ORDER BY MAX(create_time) DESC")
    List<Long> selectUserIdsByAdminId(Long adminId);

    @Select("SELECT * FROM `chat_message` WHERE session_id = #{sessionId} ORDER BY create_time ASC")
    List<ChatMessage> selectBySessionId(String sessionId);

    @Select("SELECT session_id FROM `chat_message` WHERE admin_id = #{adminId} GROUP BY session_id ORDER BY MAX(create_time) DESC")
    List<String> selectSessionsByAdminId(Long adminId);

    @Select("SELECT * FROM `chat_message` WHERE session_id = #{sessionId} ORDER BY create_time DESC LIMIT 1")
    ChatMessage selectLastMessageBySessionId(String sessionId);

    @Select("SELECT * FROM `chat_message` WHERE user_id = #{userId} AND admin_id = #{adminId} ORDER BY create_time DESC LIMIT 1")
    ChatMessage selectLastMessageByUserIdAndAdminId(@Param("userId") Long userId, @Param("adminId") Long adminId);

    @Select("SELECT COUNT(*) FROM `chat_message` WHERE session_id = #{sessionId} AND type = 0 AND is_read = 0")
    int countUnreadBySessionId(String sessionId);

    @Update("UPDATE `chat_message` SET is_read = 1 WHERE session_id = #{sessionId} AND type = 0")
    int markAsRead(String sessionId);

    @Delete("DELETE FROM `chat_message` WHERE session_id = #{sessionId}")
    int deleteBySessionId(String sessionId);
}
