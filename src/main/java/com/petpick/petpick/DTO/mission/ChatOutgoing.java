package com.petpick.petpick.DTO.mission;

public record ChatOutgoing(Long conversationId, Long messageId, Long senderId, String senderName, String content,
        String createdAt) {
}
