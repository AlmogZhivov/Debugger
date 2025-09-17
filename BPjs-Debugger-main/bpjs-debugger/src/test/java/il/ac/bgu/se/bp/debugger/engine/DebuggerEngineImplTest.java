package il.ac.bgu.se.bp.debugger.engine;

import il.ac.bgu.cs.bp.bpjs.BPjs;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.cs.bp.bpjs.model.ResourceBProgram;
import il.ac.bgu.se.bp.debugger.BPJsDebugger;
import il.ac.bgu.se.bp.debugger.DebuggerLevel;
import il.ac.bgu.se.bp.debugger.RunnerState;
import il.ac.bgu.se.bp.debugger.engine.events.BPStateEvent;
import il.ac.bgu.se.bp.debugger.engine.events.ProgramStatusEvent;
import il.ac.bgu.se.bp.execution.manage.ProgramValidatorImpl;
import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import il.ac.bgu.se.bp.socket.status.Status;
import il.ac.bgu.se.bp.utils.DebuggerStateHelper;
import il.ac.bgu.se.bp.utils.asyncHelper.AsyncOperationRunnerImpl;
import il.ac.bgu.se.bp.utils.observer.BPEvent;
import il.ac.bgu.se.bp.utils.observer.Publisher;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.util.reflection.FieldSetter;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;

public class DebuggerEngineImplTest {

    private final static String TEST_FILENAME = "BPJSTestEngine.js";
    private static final String debuggerId = "6981cb0a-f871-474b-98e9-faf7c02e18a4";

    private final static int[] BREAKPOINTS_LINES = new int[]{2, 4};
    private final static Map<Integer, Boolean> breakpoints = new HashMap<>();

    private final ExecutorService execSvc = BPjs.getExecutorServiceMaker().makeWithName("TEST_");
    private final static BlockingQueue<BPDebuggerState> onStateChangedQueue = new ArrayBlockingQueue<>(5);
    private RunnerState state = new RunnerState();
    private BPDebuggerState expectedState;
    private Stack<Status> expectedStatus;
    private boolean testStatus;

    @Mock
    private DebuggerStateHelper debuggerStateHelper;

    @Mock
    private Publisher<BPEvent> publisher;

    @Mock
    private BPJsDebugger bpJsDebugger;

    @Spy
    private AsyncOperationRunnerImpl asyncOperationRunner;

    @Spy
    private ProgramValidatorImpl programValidator;

    private DebuggerEngineImpl debuggerEngine;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        debuggerEngine = new DebuggerEngineImpl(debuggerId, TEST_FILENAME, state, debuggerStateHelper, "debuggerTestId");
        debuggerEngine.changeDebuggerLevel(DebuggerLevel.NORMAL);
        setMockPublisher();
        onStateChangedQueue.clear();
        Arrays.stream(BREAKPOINTS_LINES).forEach(lineNumber -> breakpoints.put(lineNumber, true));
        expectedStatus = new Stack<>();
        testStatus = true;
    }

    private void setMockPublisher() {
        try {
            FieldSetter.setField(debuggerEngine, DebuggerEngineImpl.class.getDeclaredField("publisher"), publisher);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    private void setMockDebugger() {
        try {
            FieldSetter.setField(debuggerStateHelper, DebuggerEngineImpl.class.getDeclaredField("bpJsDebugger"), bpJsDebugger);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEnvChangedInBreakPoints() throws InterruptedException {
        expectedStatus.push(Status.BREAKPOINT);
        expectedStatus.push(Status.BREAKPOINT);
        doAnswer(invocation -> onStateChangedTester(invocation.getArgument(0))).when(publisher).notifySubscribers(any());
        expectedState = new BPDebuggerState(new LinkedList<>(), null);
        BProgram bProg = new ResourceBProgram(TEST_FILENAME);
        BProgramSyncSnapshot bProgramSyncSnapshot = bProg.setup();
        debuggerEngine.setSyncSnapshot(bProgramSyncSnapshot);

        Arrays.stream(BREAKPOINTS_LINES).forEach(lineNumber -> debuggerEngine.setBreakpoint(lineNumber, true));

        doCallRealMethod().when(debuggerStateHelper).setRecentlyRegisteredBThreads(any());
        doCallRealMethod().when(debuggerStateHelper).getLastState();
        doCallRealMethod().when(debuggerStateHelper).peekNextState(any(), any(), any(), any());
        doCallRealMethod().when(debuggerStateHelper).cleanFields();

//TODO: fix
//        Set<BThreadSyncSnapshot> recentlyRegisteredBThreads = bProg.getRecentlyRegisteredBthreads();
//        Set<Pair<String, Object>> recentlyRegistered = new HashSet<>();
//        for (BThreadSyncSnapshot b : recentlyRegisteredBThreads) {
//            recentlyRegistered.add(new Pair<>(b.getName(), b.getEntryPoint()));
//        }
//        debuggerStateHelper.setRecentlyRegisteredBThreads(recentlyRegistered);
//        try {
//            FieldSetter.setField(debuggerStateHelper, DebuggerStateHelper.class.getDeclaredField("newBTInterpreterFrames"), new HashMap<>());
//            FieldSetter.setField(debuggerStateHelper, DebuggerStateHelper.class.getDeclaredField("syncSnapshotHolder"), new SyncSnapshotHolderImpl());
//            FieldSetter.setField(debuggerStateHelper, DebuggerStateHelper.class.getDeclaredField("debuggerLevel"), DebuggerLevel.NORMAL);
//        } catch (NoSuchFieldException e) {
//            e.printStackTrace();
//        }
//        when(debuggerStateHelper.generateDebuggerState(any(), any(), any(), any())).thenAnswer(invocationOnMock -> {
//            debuggerEngine.addCommand(new StepInto());
//            return invocationOnMock.callRealMethod();
//        });
//        try {
//            bProgramSyncSnapshot = bProgramSyncSnapshot.start(execSvc);
//            doCallRealMethod().when(debuggerStateHelper).generateDebuggerState(any(), any(), any(), any());
//            state.setDebuggerState(RunnerState.State.SYNC_STATE);
//            debuggerEngine.setSyncSnapshot(bProgramSyncSnapshot);
//            debuggerEngine.onStateChanged();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        BPDebuggerState state = onStateChangedQueue.take();
//        expectedState = ExpectedResults.testEnvChangedInBreakPoints_ENV1();
//        assertEquals(expectedState, state);
//        state = onStateChangedQueue.take();
//        expectedState = ExpectedResults.testEnvChangedInBreakPoints_ENV2();
//        assertEquals(expectedState, state);
//        debuggerEngine.onStateChanged();
//
//        state = onStateChangedQueue.take();
//        state = onStateChangedQueue.take();
//        expectedState = ExpectedResults.testEnvChangedInBreakPoints_ENV3();
//        assertEquals(expectedState, state);
    }

    private <T> Void onStateChangedTester(T event) {
        if (event instanceof BPStateEvent) {
            BPStateEvent bpStateEvent = castToMyType(event);
            assertEquals(debuggerId, bpStateEvent.getDebuggerId());
            onStateChangedQueue.add(bpStateEvent.getEvent());
        }
        else if (testStatus && event instanceof ProgramStatusEvent) {
            ProgramStatusEvent programStatusEvent = castToMyType(event);
            assertEquals(expectedStatus.pop(), programStatusEvent.getEvent().getStatus());
        }
        return null;
    }

    private <T> T castToMyType(Object object) {
        return (T) object;
    }

}