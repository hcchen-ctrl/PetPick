package com.petpick.petpick_server.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.petpick.petpick_server.entity.Message;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("select m from Message m where m.conversation.conversationId=:cid order by m.createdAt asc")
    List<Message> findAllByConversation(@Param("cid") Long conversationId);

    @Modifying
    @Query("""
              update Message m set m.readFlag = true
              where m.conversation.conversationId = :cid
                and m.sender.userId <> :uid
                and m.readFlag = false
            """)
    int markAsRead(@Param("cid") Long conversationId, @Param("uid") Long userId);

}
