package il.ac.bgu.se.bp.rest.socket.notifications.sender;

import il.ac.bgu.se.bp.service.manage.SessionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.io.Serializable;
import java.security.Principal;
import java.util.Map;
import java.util.Objects;

import static il.ac.bgu.se.bp.rest.utils.Constants.SIMP_SESSION_ID;


@Service
@Qualifier("notificationHandlerImpl")
public class NotificationSenderImpl implements NotificationSender {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private SessionHandler sessionHandler;

    public NotificationSenderImpl(SimpMessagingTemplate simpMessagingTemplate) {
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    /**
     * Handles WebSocket connection events
     */
    @EventListener(SessionConnectEvent.class)
    public void handleWebsocketConnectListener(SessionConnectEvent event) {
        System.out.println(String.format("WebSocket connection established for sessionID %s",
                getSessionIdFromMessageHeaders(event)));
    }

    private String getSessionIdFromMessageHeaders(SessionConnectEvent event) {
        Map<String, Object> headers = event.getMessage().getHeaders();
        return Objects.requireNonNull(headers.get(SIMP_SESSION_ID)).toString();
    }

    /**
     * Handles WebSocket disconnection events
     */
    @EventListener(SessionDisconnectEvent.class)
    public void handleWebsocketDisconnectListener(SessionDisconnectEvent event) {
        System.out.println(String.format("WebSocket connection closed for sessionID %s",
                getSessionIdFromMessageHeaders(event)));
        System.out.println(String.format("WebSocket connection closed for userID %s",
                getUserIdFromMessageHeaders(event)));
        sessionHandler.removeUser(getUserIdFromMessageHeaders(event));
    }

    private String getSessionIdFromMessageHeaders(SessionDisconnectEvent event) {
        Map<String, Object> headers = event.getMessage().getHeaders();
        return Objects.requireNonNull(headers.get(SIMP_SESSION_ID)).toString();
    }

    private String getUserIdFromMessageHeaders(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        assert principal != null;
        return Objects.requireNonNull(principal.getName());
    }


    @Override
    public void sendNotification(String userId, String updateURI, Serializable json) {
            simpMessagingTemplate.convertAndSendToUser(userId, updateURI, json);
    }
}
