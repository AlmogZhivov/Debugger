package il.ac.bgu.se.bp.session;

import il.ac.bgu.se.bp.socket.console.ConsoleMessage;
import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import il.ac.bgu.se.bp.socket.status.Status;

public interface ITSessionManager {

    void cleanUserMockData(String userId);
    boolean isUserFinishedRunning(String userId);

    void addUserStatus(String userId, Status userStatus);
    Status getUsersStatus(String userId);
    void removeUsersStatus(String userId);

    void addDebuggerState(String userId, BPDebuggerState debuggerState);
    BPDebuggerState getUsersLastDebuggerState(String userId);

    void addConsoleMessage(String userId, ConsoleMessage consoleMessage);


}
