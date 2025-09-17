package il.ac.bgu.se.bp.session;

import il.ac.bgu.se.bp.service.manage.SessionHandlerImpl;
import il.ac.bgu.se.bp.socket.console.ConsoleMessage;
import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import il.ac.bgu.se.bp.socket.status.ProgramStatus;
import il.ac.bgu.se.bp.socket.status.Status;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ITSessionManagerImpl extends SessionHandlerImpl implements ITSessionManager {
    private ConcurrentMap<String, List<BPDebuggerState>> debuggerStatesPerUser = new ConcurrentHashMap<>();
    private ConcurrentMap<String, List<ConsoleMessage>> consoleMessagesPerUser = new ConcurrentHashMap<>();
    private ConcurrentMap<String, Status> usersStatus = new ConcurrentHashMap<>();

    @Override
    public void visit(String userId, BPDebuggerState debuggerState) {
        addDebuggerState(userId, debuggerState);
    }

    @Override
    public void addDebuggerState(String userId, BPDebuggerState debuggerState) {
        debuggerStatesPerUser.get(userId).add(debuggerState);
    }

    @Override
    public void visit(String userId, ConsoleMessage consoleMessage) {
        addConsoleMessage(userId, consoleMessage);
    }

    @Override
    public void addConsoleMessage(String userId, ConsoleMessage consoleMessage) {
        consoleMessagesPerUser.get(userId).add(consoleMessage);
    }

    @Override
    public void visit(String userId, ProgramStatus programStatus) {
        super.visit(userId, programStatus);
        Status userStatus = programStatus.getStatus();
        addUserStatus(userId, userStatus);
    }

    @Override
    public void addUserStatus(String userId, Status userStatus) {
        if (Status.DEBUG.equals(userStatus)) {
            debuggerStatesPerUser.put(userId, new LinkedList<>());
            consoleMessagesPerUser.put(userId, new LinkedList<>());
        }
        else if (Status.STOP.equals(userStatus)) {
            debuggerStatesPerUser.remove(userId);
            consoleMessagesPerUser.remove(userId);
        }
        else if (Status.SUPERSTEPDONE.equals(userStatus)) {
            debuggerStatesPerUser.remove(userId);
            consoleMessagesPerUser.remove(userId);
        }
        usersStatus.put(userId, userStatus);
    }

    @Override
    public boolean isUserFinishedRunning(String userId) {
        return !debuggerStatesPerUser.containsKey(userId) && !consoleMessagesPerUser.containsKey(userId);
    }

    public void cleanMockData() {
        debuggerStatesPerUser.clear();
        consoleMessagesPerUser.clear();
    }

    @Override
    public void cleanUserMockData(String userId) {
        debuggerStatesPerUser.getOrDefault(userId, new LinkedList<>()).clear();
        consoleMessagesPerUser.getOrDefault(userId, new LinkedList<>()).clear();
    }

    private List<BPDebuggerState> getUsersDebuggerStates(String userId) {
        return debuggerStatesPerUser.get(userId);
    }

    @Override
    public BPDebuggerState getUsersLastDebuggerState(String userId) {
        return getLastIfNotEmpty(getUsersDebuggerStates(userId));
    }

    @Override
    public Status getUsersStatus(String userId) {
        return usersStatus.get(userId);
    }

    @Override
    public void removeUsersStatus(String userId) {
        usersStatus.remove(userId);
    }

    private List<ConsoleMessage> getUsersConsoleMessages(String userId) {
        return consoleMessagesPerUser.get(userId);
    }

    public ConsoleMessage getUsersLastConsoleMessage(String userId) {
        return getLastIfNotEmpty(getUsersConsoleMessages(userId));
    }

    private <T> T getLastIfNotEmpty(List<T> list) {
        return list == null || list.isEmpty() ? null : list.get(list.size() - 1);
    }
}
