package il.ac.bgu.se.bp.service.manage;

import il.ac.bgu.se.bp.utils.observer.BPEvent;
import il.ac.bgu.se.bp.utils.observer.Subscriber;
import il.ac.bgu.se.bp.utils.visitor.PublisherVisitor;

public interface SessionHandler<T> extends PublisherVisitor, Subscriber<BPEvent> {
    void addUser(String sessionId, String userId);

    void addNewRunExecution(String userId, T bProgramRunner, String filename);
    void addNewDebugExecution(String userId, T bpProgramDebugger, String filename);

    T getBPjsRunnerByUser(String userId);
    T getBPjsDebuggerByUser(String userId);
    T getBPjsDebuggerOrRunnerByUser(String userId);

    void updateLastOperationTime(String userId);
    void removeUser(String userId);

    boolean validateUserId(String userId);
    String getUsersSourceCode(String userId);
}