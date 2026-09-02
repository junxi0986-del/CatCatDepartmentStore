package com.ecommerce.modules.chat.mapper;

import com.ecommerce.modules.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY created_at ASC")
    List<ChatMessage> selectBySessionId(Long sessionId);

    @Insert("INSERT INTO chat_message(session_id, sender_type, sender_id, content) VALUES(#{sessionId}, #{senderType}, #{senderId}, #{content})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);
}
