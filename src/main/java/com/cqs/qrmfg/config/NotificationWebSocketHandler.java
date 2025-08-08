package com.cqs.qrmfg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationWebSocketHandler.class);
    
    // Store active sessions by username
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String username = getUsernameFromSession(session);
        if (username != null) {
            userSessions.put(username, session);
            logger.info("WebSocket connection established for user: {}", username);
            
            // Send connection confirmation
            Map<String, Object> message = new HashMap<>();
            message.put("type", "connection_established");
            message.put("message", "Real-time notifications enabled");
            sendMessage(session, message);
        } else {
            logger.warn("WebSocket connection rejected - no username provided");
            session.close();
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = getUsernameFromSession(session);
        if (username != null) {
            userSessions.remove(username);
            logger.info("WebSocket connection closed for user: {}", username);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String username = getUsernameFromSession(session);
        logger.error("WebSocket transport error for user {}: {}", username, exception.getMessage());
        
        if (username != null) {
            userSessions.remove(username);
        }
    }
    
    /**
     * Send notification to a specific user
     */
    public void sendNotificationToUser(String username, Map<String, Object> notification) {
        WebSocketSession session = userSessions.get(username);
        if (session != null && session.isOpen()) {
            sendMessage(session, notification);
        } else {
            logger.debug("No active WebSocket session for user: {}", username);
        }
    }
    
    /**
     * Send notification to multiple users
     */
    public void sendNotificationToUsers(java.util.List<String> usernames, Map<String, Object> notification) {
        for (String username : usernames) {
            sendNotificationToUser(username, notification);
        }
    }
    
    /**
     * Broadcast notification to all connected users
     */
    public void broadcastNotification(Map<String, Object> notification) {
        userSessions.forEach((username, session) -> {
            if (session.isOpen()) {
                sendMessage(session, notification);
            }
        });
    }
    
    /**
     * Get count of active connections
     */
    public int getActiveConnectionCount() {
        return (int) userSessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }
    
    /**
     * Get list of connected users
     */
    public java.util.Set<String> getConnectedUsers() {
        return userSessions.entrySet().stream()
                .filter(entry -> entry.getValue().isOpen())
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toSet());
    }
    
    private void sendMessage(WebSocketSession session, Map<String, Object> message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
        } catch (IOException e) {
            logger.error("Failed to send WebSocket message: {}", e.getMessage());
        }
    }
    
    private String getUsernameFromSession(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri != null) {
            String query = uri.getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2 && "user".equals(keyValue[0])) {
                        return keyValue[1];
                    }
                }
            }
        }
        return null;
    }
}