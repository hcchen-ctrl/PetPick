package com.petpick.petpick.service.mission;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.petpick.petpick.DTO.mission.ConversationItemDTO;
import com.petpick.petpick.DTO.mission.MessageDTO;
import com.petpick.petpick.entity.mission.Conversation;
import com.petpick.petpick.entity.mission.Message;
import com.petpick.petpick.entity.mission.Mission;
import com.petpick.petpick.repository.mission.ConversationRepository;
import com.petpick.petpick.repository.mission.MessageRepository;
import com.petpick.petpick.repository.mission.MissionRepository;
import com.petpick.petpick.repository.mission.UserinfoRepository;

@Service
public class ChatService {

    private final ConversationRepository convRepo;
    private final MessageRepository msgRepo;
    private final MissionRepository missionRepo;
    private final UserinfoRepository userRepo;

    // 使用構造函數注入
    public ChatService(ConversationRepository convRepo,
            MessageRepository msgRepo,
            MissionRepository missionRepo,
            UserinfoRepository userRepo) {
        this.convRepo = convRepo;
        this.msgRepo = msgRepo;
        this.missionRepo = missionRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public Conversation getOrCreate(Long missionId, Long applicantId) {
        // 使用自定義的 findWithAllByMissionId 方法
        Mission mission = missionRepo.findWithAllByMissionId(missionId)
                .orElseThrow(() -> new IllegalArgumentException("找不到任務，ID: " + missionId));

        Long ownerId = mission.getPoster().getUserId();

        // 檢查是否已存在對話
        var existingConv = convRepo.findByUnique(missionId, ownerId, applicantId);
        if (existingConv.isPresent()) {
            return existingConv.get();
        }

        // 驗證申請者是否存在
        var applicant = userRepo.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("找不到申請者，ID: " + applicantId));

        // 創建新對話
        Conversation conversation = new Conversation();
        conversation.setMission(mission);
        conversation.setOwner(mission.getPoster());
        conversation.setApplicant(applicant);
        conversation.setLastMessageAt(LocalDateTime.now());
        conversation.setLastMessagePreview("開始對話");

        return convRepo.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationItemDTO> listMyConversations(Long userId) {
        return convRepo.listByUser(userId).stream().map(conversation -> {
            boolean amOwner = conversation.getOwner().getUserId().equals(userId);
            var otherUser = amOwner ? conversation.getApplicant() : conversation.getOwner();

            return new ConversationItemDTO(
                    conversation.getConversationId(),
                    conversation.getMission().getMissionId(),
                    conversation.getMission().getTitle(),
                    otherUser.getUserId(),
                    otherUser.getUsername(),
                    "/api/users/avatar/" + otherUser.getUserId(),
                    conversation.getLastMessagePreview(),
                    conversation.getLastMessageAt(),
                    msgRepo.countUnread(conversation.getConversationId(), userId));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<MessageDTO> listMessages(Long conversationId, Long userId) {
        return msgRepo.findAllByConversation(conversationId).stream()
                .map(message -> new MessageDTO(
                        message.getConversation().getConversationId(),
                        message.getMessageId(),
                        message.getSender().getUserId(),
                        message.getSender().getUsername(),
                        message.getContent(),
                        message.getCreatedAt()))
                .toList();
    }

    @Transactional
    public MessageDTO send(Long conversationId, Long senderId, String content) {
        // 驗證對話是否存在
        Conversation conversation = convRepo.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("找不到對話，ID: " + conversationId));

        // 驗證發送者是否存在
        var sender = userRepo.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到發送者，ID: " + senderId));

        // 驗證發送者是否有權限在此對話中發送訊息
        if (!(sender.getUserId().equals(conversation.getOwner().getUserId())
                || sender.getUserId().equals(conversation.getApplicant().getUserId()))) {
            throw new SecurityException("用戶無權限在此對話中發送訊息");
        }

        // 創建新訊息
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);
        message = msgRepo.save(message);

        // 更新對話的最後訊息時間和預覽
        conversation.setLastMessageAt(message.getCreatedAt());
        conversation.setLastMessagePreview(content.length() > 200 ? content.substring(0, 200) : content);
        convRepo.save(conversation);

        return new MessageDTO(
                message.getConversation().getConversationId(),
                message.getMessageId(),
                senderId,
                sender.getUsername(),
                message.getContent(),
                message.getCreatedAt());
    }

    @Transactional
    public void markRead(Long conversationId, Long userId) {
        // 驗證對話是否存在
        if (!convRepo.existsById(conversationId)) {
            throw new IllegalArgumentException("找不到對話，ID: " + conversationId);
        }

        // 驗證用戶是否存在
        if (!userRepo.existsById(userId)) {
            throw new IllegalArgumentException("找不到用戶，ID: " + userId);
        }

        msgRepo.markAsRead(conversationId, userId);
    }
}