package com.petpick.petpick.DTO.mission;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
        private Long conversationId;
        private Long messageId;
        private Long senderId;
        private String senderName;
        private String content;
        private LocalDateTime createdAt;
        
}


