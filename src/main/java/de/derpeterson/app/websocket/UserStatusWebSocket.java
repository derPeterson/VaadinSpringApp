package de.derpeterson.app.websocket;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserStatusWebSocket {

    private final SimpMessagingTemplate messagingTemplate;

    public UserStatusWebSocket(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendUserStatusUpdate(Long userId, String newStatus) {
        messagingTemplate.convertAndSend("/topic/user-status", new UserStatusMessage(userId, newStatus));
    }

    public record UserStatusMessage(Long userId, String status) {
    }
}
