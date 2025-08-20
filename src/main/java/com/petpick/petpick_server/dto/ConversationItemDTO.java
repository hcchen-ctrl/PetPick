package com.petpick.petpick_server.dto;

import java.time.LocalDateTime;

public record ConversationItemDTO(
        Long conversationId, Long missionId, String missionTitle,
        Long otherUserId, String otherName, String otherAvatarUrl,
        String lastMessage, LocalDateTime lastTime) {
}
