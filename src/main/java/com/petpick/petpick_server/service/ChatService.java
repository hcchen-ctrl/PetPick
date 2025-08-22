package com.petpick.petpick_server.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petpick.petpick_server.dto.ConversationItemDTO;
import com.petpick.petpick_server.dto.MessageDTO;
import com.petpick.petpick_server.entity.Conversation;
import com.petpick.petpick_server.entity.Message;
import com.petpick.petpick_server.entity.Mission;
import com.petpick.petpick_server.repository.ConversationRepository;
import com.petpick.petpick_server.repository.MessageRepository;
import com.petpick.petpick_server.repository.MissionRepository;
import com.petpick.petpick_server.repository.UserinfoRepository;

@Service
public class ChatService {
    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final MissionRepository missionRepo;
    private final UserinfoRepository userRepo;

    public ChatService(ConversationRepository c, MessageRepository m, MissionRepository mr, UserinfoRepository ur) {
        this.convRepo = c;
        this.msgRepo = m;
        this.missionRepo = mr;
        this.userRepo = ur;
    }

    @Transactional
    public Conversation getOrCreate(Long missionId, Long applicantId) {
        Mission mission = missionRepo.findById(missionId).orElseThrow();
        Long ownerId = mission.getPoster().getUserId();
        var opt = convRepo.findByUnique(missionId, ownerId, applicantId);
        if (opt.isPresent())
            return opt.get();
        Conversation c = new Conversation();
        c.setMission(mission);
        c.setOwner(mission.getPoster());
        c.setApplicant(userRepo.findById(applicantId).orElseThrow());
        c.setLastMessageAt(LocalDateTime.now());
        c.setLastMessagePreview("開始對話");
        return convRepo.save(c);
    }

    @Transactional(readOnly = true)
    public List<ConversationItemDTO> listMyConversations(Long userId) {
        return convRepo.listByUser(userId).stream().map(c -> {
            boolean amOwner = c.getOwner().getUserId().equals(userId);
            var other = amOwner ? c.getApplicant() : c.getOwner();
            return new ConversationItemDTO(
                    c.getConversationId(),
                    c.getMission().getMissionId(),
                    c.getMission().getTitle(),
                    other.getUserId(),
                    other.getUsername(),
                    "/api/users/avatar/" + other.getUserId(),
                    c.getLastMessagePreview(),
                    c.getLastMessageAt(),
                    msgRepo.countUnread(c.getConversationId(), userId)
            );
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> listMessages(Long conversationId, Long userId) {
        return msgRepo.findAllByConversation(conversationId).stream()
                .map(m -> new MessageDTO(
                        m.getMessageId(),
                        m.getConversation().getConversationId(),
                        m.getSender().getUserId(),
                        m.getSender().getUsername(),
                        m.getContent(),
                        m.getCreatedAt()))
                .toList();
    }

    @Transactional
    public MessageDTO send(Long conversationId, Long senderId, String text) {
        Conversation c = convRepo.findById(conversationId).orElseThrow();
        var sender = userRepo.findById(senderId).orElseThrow();
        if (!(sender.getUserId().equals(c.getOwner().getUserId())
                || sender.getUserId().equals(c.getApplicant().getUserId())))
            throw new SecurityException("not in conversation");

        Message msg = new Message();
        msg.setConversation(c);
        msg.setSender(sender);
        msg.setContent(text);
        msg = msgRepo.save(msg);

        c.setLastMessageAt(msg.getCreatedAt());
        c.setLastMessagePreview(text.length() > 200 ? text.substring(0, 200) : text);

        return new MessageDTO(
                msg.getConversation().getConversationId(),
                msg.getMessageId(),
                senderId,
                sender.getUsername(),
                msg.getContent(),
                msg.getCreatedAt());
    }

    @Transactional
    public void markRead(Long conversationId, Long userId) {
        msgRepo.markAsRead(conversationId, userId);
    }
}
