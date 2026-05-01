package com.harbr.messaging.domain;

import com.harbr.auth.domain.User;
import com.harbr.common.persistence.BaseEntity;
import com.harbr.property.domain.Property;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "conversations", uniqueConstraints = {
        @UniqueConstraint(name = "uq_conversations_property_guest_host", columnNames = {"property_id", "guest_id", "host_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private User guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(name = "last_message_content")
    private String lastMessageContent;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;
}