package com.petpick.petpick.DTO.mission;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConversationItemDTO {
        private Long conversationId;
        private Long missionId;
        private String missionTitle;
        private Long otherUserId;
        private String otherName;
        private String otherAvatarUrl;
        private String lastMessage;
        private LocalDateTime lastTime;
        private long unreadCount; 
}
