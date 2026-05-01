package com.harbr.messaging.infrastructure;

import com.harbr.messaging.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByPropertyIdAndGuestIdAndHostId(UUID propertyId, UUID guestId, UUID hostId);

    @Query("SELECT c FROM Conversation c " +
            "WHERE c.guest.id = :userId OR c.host.id = :userId " +
            "ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<Conversation> findByParticipantIdOrderByLastMessageAtDesc(UUID userId);
}