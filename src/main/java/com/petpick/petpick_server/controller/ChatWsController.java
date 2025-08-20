package com.petpick.petpick_server.controller;

import java.time.LocalDateTime;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.RestController;

import com.petpick.petpick_server.dto.ChatWSPayload;
import com.petpick.petpick_server.dto.ChatWSPayload.ChatEvent;
import com.petpick.petpick_server.dto.ChatWSPayload.ReadReq;
import com.petpick.petpick_server.dto.ChatWSPayload.TypingReq;
import com.petpick.petpick_server.dto.MessageDTO;
import com.petpick.petpick_server.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ChatWsController {

    private final ChatService chatService;
    private final SimpMessagingTemplate broker;

    @MessageMapping("/chat.send")
    public void send(@Payload ChatWSPayload.ChatSendReq req) {
        log.debug("[WS] /app/chat.send -> cid={}, sender={}, content={}", req.conversationId(), req.senderId(), req.content());
        MessageDTO dto = chatService.send(req.conversationId(), req.senderId(), req.content());
        ChatWSPayload.ChatEvent ev = new ChatEvent(dto.getConversationId(), "message",
                dto.getMessageId(), dto.getSenderId(), dto.getSenderName(),
                dto.getContent(), dto.getCreatedAt(), null);
        broker.convertAndSend("/topic/conversations." + dto.getConversationId(), ev);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingReq req) {
        log.debug("[WS] /app/chat.typing -> cid={}, sender={}", req.conversationId(), req.senderId());
        ChatEvent ev = new ChatEvent(
                req.conversationId(), "typing",
                null, req.senderId(), null,
                null, LocalDateTime.now(), null
        );
        broker.convertAndSend("/topic/conversations." + req.conversationId(), ev);
    }

    @MessageMapping("/chat.read")
    public void read(@Payload ReadReq req) {
        log.debug("[WS] /app/chat.read -> cid={}, reader={}", req.conversationId(), req.userId());
        chatService.markRead(req.conversationId(), req.userId());
        ChatEvent ev = new ChatEvent(
                req.conversationId(), "read",
                null, null, null,
                null, LocalDateTime.now(), req.userId()
        );
        broker.convertAndSend("/topic/conversations." + req.conversationId(), ev);
    }
}
