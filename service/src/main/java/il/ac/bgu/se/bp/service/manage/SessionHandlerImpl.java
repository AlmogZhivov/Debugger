package il.ac.bgu.se.bp.service.manage;

import il.ac.bgu.se.bp.debugger.BPJsDebugger;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.service.code.SourceCodeHelper;
import il.ac.bgu.se.bp.service.notification.NotificationHandler;
import il.ac.bgu.se.bp.socket.console.ConsoleMessage;
import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import il.ac.bgu.se.bp.socket.status.ProgramStatus;
import il.ac.bgu.se.bp.socket.status.Status;
import il.ac.bgu.se.bp.utils.logger.Logger;
import il.ac.bgu.se.bp.utils.observer.BPEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@EnableScheduling
public class SessionHandlerImpl implements SessionHandler<BPJsDebugger<BooleanResponse>> {

    private static final Logger logger = new Logger(SessionHandlerImpl.class);

    private static final int ONE_HOUR = 60 * 60 * 1000;
    private static final int BP_JS_PROGRAM_TTL = 3 * ONE_HOUR;

    private static final Map<String, UserProgramSession<BPJsDebugger<BooleanResponse>>> bpDebugProgramsByUsers = new ConcurrentHashMap<>();
    private static final Map<String, UserProgramSession<BPJsDebugger<BooleanResponse>>> bpRunProgramsByUsers = new ConcurrentHashMap<>();
    private static final Map<String, UserSession> unknownSessions = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("stateNotificationHandlerImpl")
    private NotificationHandler stateNotificationHandler;

    @Autowired
    @Qualifier("consoleNotificationHandlerImpl")
    private NotificationHandler consoleNotificationHandler;

    @Autowired
    @Qualifier("programStatusNotificationHandlerImpl")
    private NotificationHandler programStatusNotificationHandler;

    @Autowired
    private SourceCodeHelper sourceCodeHelper;

    @Autowired
    private PrototypeContextFactory prototypeContextFactory;

    @Override
    public void addUser(String sessionId, String userId) {
        logger.info("adding user: {0}", userId);
        unknownSessions.put(userId, new UserSession(sessionId, userId, getCurrentLocalDateTime()));
    }

    @Override
    public void addNewRunExecution(String userId, BPJsDebugger<BooleanResponse> bpProgramDebugger, String filename) {
        addDebugExecutionTo(userId, bpProgramDebugger, filename, bpRunProgramsByUsers);
    }

    @Override
    public void addNewDebugExecution(String userId, BPJsDebugger<BooleanResponse> bpProgramDebugger, String filename) {
        addDebugExecutionTo(userId, bpProgramDebugger, filename, bpDebugProgramsByUsers);
    }

    private void addDebugExecutionTo(String userId, BPJsDebugger<BooleanResponse> bpProgramDebugger, String filename, Map<String, UserProgramSession<BPJsDebugger<BooleanResponse>>> bpDebuggersByUsers) {
        UserSession existingUserSession = unknownSessions.get(userId);
        if (existingUserSession == null) {
            return;
        }

        existingUserSession.setThreadId(bpProgramDebugger.getDebuggerExecutorId());
        bpDebuggersByUsers.put(userId, existingUserSession.withProgram(bpProgramDebugger).withFilename(filename));
        updateLastOperationTime(userId);
    }

    @Override
    public BPJsDebugger<BooleanResponse> getBPjsRunnerByUser(String userId) {
        return getBPJsDebuggerFrom(userId, bpRunProgramsByUsers);
    }

    @Override
    public BPJsDebugger<BooleanResponse> getBPjsDebuggerByUser(String userId) {
        return getBPJsDebuggerFrom(userId, bpDebugProgramsByUsers);
    }

    @Override
    public BPJsDebugger<BooleanResponse> getBPjsDebuggerOrRunnerByUser(String userId) {
        UserProgramSession<BPJsDebugger<BooleanResponse>> userSession = getUserDebuggerSession(userId);
        return userSession != null ? userSession.getProgram() : null;
    }

    private BPJsDebugger<BooleanResponse> getBPJsDebuggerFrom(String userId, Map<String, UserProgramSession<BPJsDebugger<BooleanResponse>>> bpDebuggersByUsers) {
        UserProgramSession<BPJsDebugger<BooleanResponse>> userSession = bpDebuggersByUsers.get(userId);
        if (userSession == null) {
            return null;
        }

        return userSession.getProgram();
    }

    @Override
    public boolean validateUserId(String userId) {
        return !StringUtils.isEmpty(userId) &&
                getUserSession(userId) != null;
    }

    @Override
    public String getUsersSourceCode(String userId) {
        UserProgramSession<BPJsDebugger<BooleanResponse>> userSession = getUserDebuggerSession(userId);
        return userSession != null ? sourceCodeHelper.readCodeFile(userSession.getFilename()) : null;
    }

    private UserSession getUserSession(String userId) {
        UserSession userSession = unknownSessions.get(userId);
        userSession = userSession == null ? bpRunProgramsByUsers.get(userId) : userSession;
        return userSession == null ? bpDebugProgramsByUsers.get(userId) : userSession;
    }

    private UserProgramSession<BPJsDebugger<BooleanResponse>> getUserDebuggerSession(String userId) {
        UserProgramSession<BPJsDebugger<BooleanResponse>> userSession = bpRunProgramsByUsers.get(userId);
        return userSession == null ? bpDebugProgramsByUsers.get(userId) : userSession;
    }


    @Override
    public void updateLastOperationTime(String userId) {
        updateLastOperationTime(unknownSessions, userId);
        updateLastOperationTime(bpRunProgramsByUsers, userId);
        updateLastOperationTime(bpDebugProgramsByUsers, userId);
    }

    private void updateLastOperationTime(Map<String, ? extends UserSession> sessions, String userId) {
        UserSession userSession = sessions.get(userId);
        if (userSession == null) {
            return;
        }

        userSession.setLastOperationTime(getCurrentLocalDateTime());
    }

    @Override
    public void removeUser(String userId) {
        logger.info("removing user: {0}", userId);
        unknownSessions.remove(userId);
        removeUserPrograms(userId);
    }

    @Scheduled(fixedRate = BP_JS_PROGRAM_TTL)
    private void cleanProgramsReachedThreshold() {
        verifySessionsThreshold(unknownSessions, BP_JS_PROGRAM_TTL);
        verifySessionsThreshold(bpRunProgramsByUsers, BP_JS_PROGRAM_TTL);
        verifySessionsThreshold(bpDebugProgramsByUsers, BP_JS_PROGRAM_TTL);
    }

    private void verifySessionsThreshold(Map<String, ? extends UserSession> userSessionsByIds, int threshold) {
        LocalDateTime currentTime = getCurrentLocalDateTime();
        userSessionsByIds.forEach((userId, userSession) -> {
            if (!userSession.getLastOperationTime().plusHours(threshold).isAfter(currentTime)) {
                removeUser(userId);
            }
        });
    }

    @Override
    public void visit(String userId, BPDebuggerState debuggerState) {
        if (!validateUserId(userId)) {
            return;
        }
        stateNotificationHandler.sendNotification(userId, debuggerState);
    }

    @Override
    public void visit(String userId, ConsoleMessage consoleMessage) {
        if (!validateUserId(userId)) {
            return;
        }
        consoleNotificationHandler.sendNotification(userId, consoleMessage);
    }

    @Override
    public void visit(String userId, ProgramStatus programStatus) {
        if (!validateUserId(userId)) {
            return;
        }
        programStatusNotificationHandler.sendNotification(userId, programStatus);
        if (Status.STOP.equals(programStatus.getStatus())) {
            removeUserPrograms(userId);
        }
    }

    @Override
    public void update(BPEvent event) {
        event.accept(this);
    }

    private void removeUserPrograms(String userId) {
        logger.info("removing programs associated with user: {0}", userId);
        removeUserProgramFrom(userId, bpDebugProgramsByUsers);
        removeUserProgramFrom(userId, bpRunProgramsByUsers);
    }

    private void removeUserProgramFrom(String userId, Map<String, ? extends UserProgramSession> programByUserId) {
        UserProgramSession userProgramSession = programByUserId.remove(userId);
        if (userProgramSession != null) {
            prototypeContextFactory.removeThread(userProgramSession.getThreadId());
            sourceCodeHelper.removeCodeFile(userProgramSession.getFilename());
        }
    }

    private LocalDateTime getCurrentLocalDateTime() {
        return LocalDateTime.now();
    }
}
