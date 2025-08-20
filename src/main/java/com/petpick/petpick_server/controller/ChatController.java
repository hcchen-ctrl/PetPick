package com.petpick.petpick_server.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick_server.dto.ConversationItemDTO;
import com.petpick.petpick_server.dto.MessageDTO;
import com.petpick.petpick_server.service.ChatService;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService svc;

    public ChatController(ChatService s) {
        this.svc = s;
    }

    // 建立任務對話
    @PostMapping("/conversations")
    public Long getOrCreate(@RequestParam Long missionId, @RequestParam Long applicantId) {
        return svc.getOrCreate(missionId, applicantId).getConversationId();
    }

    // 對話清單（側邊列表）
    @GetMapping("/conversations")
    public List<ConversationItemDTO> myConversations(@RequestParam Long userId) {
        return svc.listMyConversations(userId);
    }

    // 讀取訊息
    @GetMapping("/conversations/{id}/messages")
    public List<MessageDTO> messages(@PathVariable Long id, @RequestParam Long userId) {
        return svc.listMessages(id, userId);
    }

    // 發送訊息
    public static record SendReq(Long senderId, String content) {
    }

    @PostMapping("/conversations/{id}/messages")
    public MessageDTO send(@PathVariable Long id, @RequestBody SendReq req) {
        return svc.send(id, req.senderId(), req.content());
    }

     @PostMapping("/conversations/{id}/read")
    public void markRead(@PathVariable Long id, @RequestParam Long userId) {
        svc.markRead(id, userId);
    }
}
