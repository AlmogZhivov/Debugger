package il.ac.bgu.se.bp.service.notification;


import java.io.Serializable;

@FunctionalInterface
public interface NotificationHandler {
    void sendNotification(String userId, Serializable json);
}
