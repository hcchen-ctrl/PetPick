package com.petpick.petpick_server.dto;

public record ChatOutgoing(Long conversationId, Long messageId, Long senderId, String senderName, String content,
        String createdAt) {
}
