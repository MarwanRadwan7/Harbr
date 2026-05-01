package com.harbr.messaging.application;

import com.harbr.auth.domain.User;
import com.harbr.auth.infrastructure.UserRepository;
import com.harbr.common.exception.BusinessException;
import com.harbr.common.exception.EntityNotFoundException;
import com.harbr.messaging.application.dto.ConversationResponse;
import com.harbr.messaging.application.dto.CreateConversationRequest;
import com.harbr.messaging.application.dto.MessageResponse;
import com.harbr.messaging.application.dto.SendMessageRequest;
import com.harbr.messaging.domain.Conversation;
import com.harbr.messaging.domain.Message;
import com.harbr.messaging.infrastructure.ConversationRepository;
import com.harbr.messaging.infrastructure.MessageRepository;
import com.harbr.property.domain.Property;
import com.harbr.property.infrastructure.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    @Transactional
    public ConversationResponse createConversation(UUID guestId, CreateConversationRequest request) {
        Property property = propertyRepository.findById(request.propertyId())
                .filter(p -> p.getDeletedAt() == null)
                .orElseThrow(() -> new EntityNotFoundException("Property", request.propertyId()));

        UUID hostId = property.getHost().getId();

        if (hostId.equals(guestId)) {
            throw new BusinessException("INVALID_CONVERSATION", "Cannot start a conversation with yourself");
        }

        Conversation conversation = conversationRepository
                .findByPropertyIdAndGuestIdAndHostId(property.getId(), guestId, hostId)
                .orElseGet(() -> {
                    User guest = userRepository.findById(guestId)
                            .orElseThrow(() -> new EntityNotFoundException("User", guestId));

                    Conversation newConv = Conversation.builder()
                            .property(property)
                            .guest(guest)
                            .host(property.getHost())
                            .lastMessageContent(request.initialMessage())
                            .lastMessageAt(Instant.now())
                            .build();
                    return conversationRepository.save(newConv);
                });

        Message message = Message.builder()
                .conversation(conversation)
                .sender(userRepository.findById(guestId).orElseThrow())
                .content(request.initialMessage())
                .build();
        messageRepository.save(message);

        if (conversation.getLastMessageAt() == null || !conversation.getId().equals(conversation.getId())) {
            conversation.setLastMessageContent(request.initialMessage());
            conversation.setLastMessageAt(Instant.now());
            conversationRepository.save(conversation);
        }

        log.info("Conversation created/retrieved: id={}, property={}, guest={}, host={}",
                conversation.getId(), property.getId(), guestId, hostId);

        return toConversationResponse(conversation, 0L);
    }

    @Transactional
    public MessageResponse sendMessage(UUID senderId, SendMessageRequest request) {
        Conversation conversation = conversationRepository.findById(request.conversationId())
                .orElseThrow(() -> new EntityNotFoundException("Conversation", request.conversationId()));

        if (!conversation.getGuest().getId().equals(senderId) &&
                !conversation.getHost().getId().equals(senderId)) {
            throw new BusinessException("FORBIDDEN", "You are not a participant in this conversation");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("User", senderId));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.content())
                .build();

        message = messageRepository.save(message);

        conversation.setLastMessageContent(request.content());
        conversation.setLastMessageAt(Instant.now());
        conversationRepository.save(conversation);

        log.info("Message sent: id={}, conversation={}, sender={}", message.getId(), conversation.getId(), senderId);

        return toMessageResponse(message);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversations(UUID userId) {
        List<Conversation> conversations = conversationRepository.findByParticipantIdOrderByLastMessageAtDesc(userId);
        return conversations.stream()
                .map(c -> {
                    Long unread = messageRepository.countByConversationIdAndIsReadFalseAndSenderIdNot(
                            c.getId(), false, userId);
                    return toConversationResponse(c, unread);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<MessageResponse> listMessages(UUID conversationId, UUID userId, int page, int size) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));

        if (!conversation.getGuest().getId().equals(userId) &&
                !conversation.getHost().getId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "You are not a participant in this conversation");
        }

        return messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(page, size))
                .map(this::toMessageResponse);
    }

    @Transactional
    public void markAsRead(UUID conversationId, UUID userId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new EntityNotFoundException("Conversation", conversationId));

        if (!conversation.getGuest().getId().equals(userId) &&
                !conversation.getHost().getId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "You are not a participant in this conversation");
        }

        List<Message> unreadMessages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, PageRequest.of(0, 100))
                .stream()
                .filter(m -> !m.getSender().getId().equals(userId) && !m.getIsRead())
                .toList();

        unreadMessages.forEach(m -> m.setIsRead(true));
        messageRepository.saveAll(unreadMessages);

        log.info("Marked {} messages as read in conversation {} for user {}", unreadMessages.size(), conversationId, userId);
    }

    private ConversationResponse toConversationResponse(Conversation c, Long unreadCount) {
        return new ConversationResponse(
                c.getId(),
                c.getProperty().getId(),
                c.getProperty().getTitle(),
                c.getGuest().getId(),
                c.getGuest().getFullName(),
                c.getHost().getId(),
                c.getHost().getFullName(),
                c.getLastMessageContent(),
                c.getLastMessageAt(),
                unreadCount,
                c.getCreatedAt()
        );
    }

    private MessageResponse toMessageResponse(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getConversation().getId(),
                m.getSender().getId(),
                m.getSender().getFullName(),
                m.getContent(),
                m.getIsRead(),
                m.getCreatedAt()
        );
    }
}