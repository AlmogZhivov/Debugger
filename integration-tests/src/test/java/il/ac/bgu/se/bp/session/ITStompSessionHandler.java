package il.ac.bgu.se.bp.session;

import il.ac.bgu.se.bp.debugger.engine.events.BPConsoleEvent;
import il.ac.bgu.se.bp.debugger.engine.events.BPStateEvent;
import il.ac.bgu.se.bp.debugger.engine.events.ProgramStatusEvent;
import il.ac.bgu.se.bp.socket.console.ConsoleMessage;
import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import il.ac.bgu.se.bp.socket.state.EventInfo;
import il.ac.bgu.se.bp.socket.status.ProgramStatus;
import il.ac.bgu.se.bp.utils.observer.BPEvent;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;

import java.lang.reflect.Type;
import java.util.SortedMap;
import java.util.TreeMap;

import static il.ac.bgu.se.bp.rest.utils.Endpoints.*;
import static org.springframework.messaging.simp.stomp.StompHeaders.DESTINATION;

public class ITStompSessionHandler implements StompSessionHandler {

    private static final String USER = "/user";
    private static final String USER_ID = "user-name";
    private static final String SUBSCRIPTION_URI = "/bpjs/subscribe";
    private final ITSessionManagerImpl usersSessionHandler;

    private String serverUserId;
    private String testUserId;
    private boolean isConnected;

    public ITStompSessionHandler(ITSessionManagerImpl usersSessionHandler) {
        this.usersSessionHandler = usersSessionHandler;
    }

    public String getServerUserId() {
        return serverUserId;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setTestUserId(String testUserId) {
        this.testUserId = testUserId;
    }

    @Override
    public void afterConnected(StompSession stompSession, StompHeaders stompHeaders) {
        serverUserId = stompHeaders.get(USER_ID).get(0);
        stompSession.subscribe(USER + CONSOLE_UPDATE, this);
        stompSession.subscribe(USER + STATE_UPDATE, this);
        stompSession.subscribe(USER + PROGRAM_UPDATE, this);
        stompSession.send(SUBSCRIPTION_URI, null);
        isConnected = true;
    }

    @Override
    public void handleException(StompSession stompSession, StompCommand stompCommand, StompHeaders stompHeaders, byte[] bytes, Throwable throwable) {
        System.err.println("failed parsing incoming socket message, error: " + throwable.getMessage());
    }

    @Override
    public void handleTransportError(StompSession stompSession, Throwable throwable) {
        System.err.println("handleTransportError, error: " + throwable.getMessage());
    }

    @Override
    public Type getPayloadType(StompHeaders stompHeaders) {
        String destination = stompHeaders.get(DESTINATION).get(0);
        if (destination.contains(CONSOLE_UPDATE)) {
            return ConsoleMessage.class;
        }
        else if (destination.contains(STATE_UPDATE)) {
            return BPDebuggerState.class;
        }
        else if (destination.contains(PROGRAM_UPDATE)) {
            return ProgramStatus.class;
        }
        return null;
    }

    @Override
    public void handleFrame(StompHeaders stompHeaders, Object object) {
        BPEvent bpEvent = createBPEvent(object);
        assert bpEvent != null;
        bpEvent.accept(usersSessionHandler);
    }

    private BPEvent createBPEvent(Object object) {
        if (object instanceof ConsoleMessage) {
            return new BPConsoleEvent(testUserId, (ConsoleMessage) object);
        }
        else if (object instanceof BPDebuggerState) {
            BPStateEvent bpStateEvent = new BPStateEvent(testUserId, (BPDebuggerState) object);
            SortedMap<Long, EventInfo> eventsHistory = bpStateEvent.getEvent().getEventsHistory();
            bpStateEvent.getEvent().setEventsHistory(new TreeMap<>(eventsHistory).descendingMap());
            return bpStateEvent;
        }
        else if (object instanceof ProgramStatus) {
            return new ProgramStatusEvent(testUserId, ((ProgramStatus) object).getStatus());
        }
        return null;
    }
}
