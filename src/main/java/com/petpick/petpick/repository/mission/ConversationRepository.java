package com.petpick.petpick.repository.mission;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.petpick.petpick.entity.mission.Conversation;


@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
            SELECT c FROM Conversation c
            JOIN FETCH c.mission m
            WHERE c.owner.userId = :uid OR c.applicant.userId = :uid
            ORDER BY coalesce(c.lastMessageAt, c.createdAt) DESC
            """)
    List<Conversation> listByUser(@Param("uid") Long userId);

    @Query("""
              SELECT c FROM Conversation c
              WHERE c.mission.missionId = :mid
                AND c.owner.userId = :ownerId
                AND c.applicant.userId = :applicantId
            """)
    Optional<Conversation> findByUnique(@Param("mid") Long missionId,
            @Param("ownerId") Long ownerId,
            @Param("applicantId") Long applicantId);
}
