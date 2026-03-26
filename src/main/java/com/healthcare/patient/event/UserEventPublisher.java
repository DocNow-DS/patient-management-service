package com.healthcare.patient.event;

import com.healthcare.patient.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange userEventsExchange;

    public UserEventPublisher(RabbitTemplate rabbitTemplate, TopicExchange userEventsExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.userEventsExchange = userEventsExchange;
    }

    public void publishUserCreated(User user) {
        publish("USER_CREATED", "user.created", user);
    }

    public void publishUserUpdated(User user) {
        publish("USER_UPDATED", "user.updated", user);
    }

    public void publishUserDeleted(User user) {
        publish("USER_DELETED", "user.deleted", user);
    }

    private void publish(String type, String routingKey, User user) {
        if (user == null) return;

        Set<String> roles = user.getRoles() == null
                ? Set.of()
                : user.getRoles().stream().map(Enum::name).collect(Collectors.toSet());

        UserEvent event = UserEvent.builder()
                .type(type)
                .occurredAt(Instant.now())
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .enabled(user.isEnabled())
                .build();

        try {
            rabbitTemplate.convertAndSend(userEventsExchange.getName(), routingKey, event);
        } catch (Exception e) {
            // Best-effort publish: Admin service should still function if broker is down.
            log.warn("Failed to publish user event {} (routingKey={})", type, routingKey, e);
        }
    }
}
