package com.petpick.petpick_server.dto;

import java.time.LocalDateTime;


public class ChatWSPayload {
    public record ChatSendReq(Long conversationId, Long senderId, String content) {
    }

    public record TypingReq(Long conversationId, Long senderId) {
    }

    public record ReadReq(Long conversationId, Long userId) {
    }

    public record ChatEvent(
            Long conversationId, String type, // "message" | "typing" | "read"
            Long messageId, Long senderId, String senderName,
            String content, LocalDateTime createdAt, Long userId // read 用
    ) {
    }
}
