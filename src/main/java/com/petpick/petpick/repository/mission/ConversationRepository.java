package com.petpick.petpick.repository.mission;

import java.util.List;
import java.util.Optional;

import com.petpick.petpick.entity.mission.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("""
            select c from Conversation c
            join fetch c.mission m
            where c.owner.userId = :uid or c.applicant.userId = :uid
            order by coalesce(c.lastMessageAt, c.createdAt) desc
            """)
    List<Conversation> listByUser(@Param("uid") Long userId);

    @Query("""
              select c from Conversation c
              where c.mission.missionId = :mid
                and c.owner.userId = :ownerId
                and c.applicant.userId = :applicantId
            """)
    Optional<Conversation> findByUnique(@Param("mid") Long missionId,
            @Param("ownerId") Long ownerId,
            @Param("applicantId") Long applicantId);
}
