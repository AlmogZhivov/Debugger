package il.ac.bgu.se.bp.execution;

import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import il.ac.bgu.se.bp.debugger.RunnerState;
import il.ac.bgu.se.bp.debugger.commands.Continue;
import il.ac.bgu.se.bp.debugger.commands.StepInto;
import il.ac.bgu.se.bp.debugger.commands.StepOut;
import il.ac.bgu.se.bp.debugger.commands.StepOver;
import il.ac.bgu.se.bp.debugger.engine.DebuggerEngine;
import il.ac.bgu.se.bp.debugger.manage.ProgramValidator;
import il.ac.bgu.se.bp.error.ErrorCode;
import il.ac.bgu.se.bp.execution.manage.ProgramValidatorImpl;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.GetSyncSnapshotsResponse;
import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import il.ac.bgu.se.bp.utils.asyncHelper.AsyncOperationRunner;
import il.ac.bgu.se.bp.utils.asyncHelper.AsyncOperationRunnerImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.function.Predicate;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class BPJsDebuggerImplTest {

    private final static String VALID_TEST_FILE = "TestCodeFile.js";
    private final static String INVALID_TEST_FILE = "InvalidCode.js";
    private static final String debuggerId = "6981cb0a-f871-474b-98e9-faf7c02e18a4";

    private final static int[] BREAKPOINTS_LINES = new int[]{2, 4};
    private final static Map<Integer, Boolean> breakpoints = new HashMap<>();

    private final static BlockingQueue<BPDebuggerState> onStateChangedQueue = new ArrayBlockingQueue<>(5);

    private ArgumentCaptor<BProgram> bProgramArgumentCaptor;

    @InjectMocks
    private BPJsDebuggerImpl bpJsDebugger = new BPJsDebuggerImpl(debuggerId, VALID_TEST_FILE);

    @Mock
    private DebuggerEngine debuggerEngine;

    @Spy
    private AsyncOperationRunner asyncOperationRunner = new AsyncOperationRunnerImpl();

    @Spy
    private ProgramValidator programValidator = new ProgramValidatorImpl();

    @Before
    public void setUp() throws NoSuchFieldException {
        MockitoAnnotations.initMocks(this);
        onStateChangedQueue.clear();
        Arrays.stream(BREAKPOINTS_LINES).forEach(lineNumber -> breakpoints.put(lineNumber, Boolean.TRUE));
        doAnswer(a -> onStateChangedTester(new BPDebuggerState())).when(debuggerEngine).onStateChanged();
        when(debuggerEngine.getBreakpoints()).thenReturn(new boolean[0]);
        FieldSetter.setField(programValidator, ProgramValidatorImpl.class.getDeclaredField("asyncOperationRunner"), asyncOperationRunner);
    }

    private void setupDebugger() {
        assertFalse(bpJsDebugger.isSetup());
        BooleanResponse booleanResponse = bpJsDebugger.setup(breakpoints, false, false, false);
        assertSuccessResponse(booleanResponse);
        assertTrue(bpJsDebugger.isSetup());

        verify(debuggerEngine, times(1)).setupBreakpoints(breakpoints);
        verify(debuggerEngine, times(1)).setSyncSnapshot(any());
    }

    @Test(expected = IllegalArgumentException.class)
    public void debugInvalidFile() {
        new BPJsDebuggerImpl(debuggerId, "invalid_file_name__a.js");
    }

    @Test
    public void debugInvalidCode() {
        BPJsDebuggerImpl debugger = new BPJsDebuggerImpl(debuggerId, INVALID_TEST_FILE);
        BooleanResponse booleanResponse = debugger.setup(breakpoints, false, false, false);
        assertErrorResponse(booleanResponse, ErrorCode.BP_SETUP_FAIL);
        assertFalse(bpJsDebugger.isSetup());
        assertFalse(bpJsDebugger.isStarted());
    }

    @Test
    public void addExternalEventTest() {
        setupDebugger();
        assertSuccessResponse(bpJsDebugger.addExternalEvent("event_1"));
        assertErrorResponse(bpJsDebugger.addExternalEvent(null), ErrorCode.INVALID_EVENT);
        assertErrorResponse(bpJsDebugger.addExternalEvent(""), ErrorCode.INVALID_EVENT);
    }

    @Test
    public void removeExternalEventTest() {
        setupDebugger();
        assertErrorResponse(bpJsDebugger.removeExternalEvent(null), ErrorCode.INVALID_EVENT);
        assertErrorResponse(bpJsDebugger.removeExternalEvent(""), ErrorCode.INVALID_EVENT);

        assertSuccessResponse(bpJsDebugger.addExternalEvent("event_1"));
        assertSuccessResponse(bpJsDebugger.removeExternalEvent("event_1"));
    }

    @Test
    public void setWaitForExternalEvents() {
        setupDebugger();
        assertSuccessResponse(bpJsDebugger.toggleWaitForExternalEvents(true));
        assertSuccessResponse(bpJsDebugger.toggleWaitForExternalEvents(false));
    }

    @Test
    public void setIsSkipSyncPointsTest() {
        setupDebugger();
        assertSuccessResponse(bpJsDebugger.toggleMuteSyncPoints(true));
        assertSuccessResponse(bpJsDebugger.toggleMuteSyncPoints(false));
    }

    @Test
    public void getSyncSnapshotsHistory_noSnapshotsAddedTest() {
        setupDebugger();
        GetSyncSnapshotsResponse getSyncSnapshotsResponse = bpJsDebugger.getSyncSnapshotsHistory();
        assertNotNull(getSyncSnapshotsResponse);
        SortedMap<Long, BPDebuggerState> syncSnapshotsHistory = getSyncSnapshotsResponse.getSyncSnapShotsHistory();
        assertNotNull(syncSnapshotsHistory);
        assertTrue(syncSnapshotsHistory.isEmpty());
    }

    @Test
    public void setSyncSnapshots_noSnapshotsAddedTest() throws InterruptedException {
        setupDebugger();
        assertErrorResponse(bpJsDebugger.setSyncSnapshot(123123L), ErrorCode.NOT_IN_BP_SYNC_STATE);
        assertErrorResponse(bpJsDebugger.setSyncSnapshot(-1L), ErrorCode.NOT_IN_BP_SYNC_STATE);

        assertSuccessResponse(bpJsDebugger.startSync(new HashMap<>(), false, false, false));
        sleepUntil(e -> !onStateChangedQueue.isEmpty(), 3);
        assertErrorResponse(bpJsDebugger.setSyncSnapshot(123123L), ErrorCode.CANNOT_REPLACE_SNAPSHOT);
        assertErrorResponse(bpJsDebugger.setSyncSnapshot(-1L), ErrorCode.CANNOT_REPLACE_SNAPSHOT);
    }

    @Test
    public void startSyncTest() throws InterruptedException {
        setupDebugger();
        assertSuccessResponse(bpJsDebugger.startSync(new HashMap<>(), false, false, false));

        sleepUntil(e -> bpJsDebugger.isStarted(), 3);
        assertTrue(bpJsDebugger.isStarted());

        assertSuccessResponse(bpJsDebugger.stop());
        sleepUntil(e -> !bpJsDebugger.isStarted(), 3);
        assertFalse(bpJsDebugger.isStarted());
    }

    @Test
    public void nextSyncTest() throws InterruptedException {
        setupDebugger();
        assertSuccessResponse(bpJsDebugger.startSync(new HashMap<>(), false, false, false));

        sleepUntil(e -> bpJsDebugger.isStarted(), 3);
        assertTrue(bpJsDebugger.isStarted());

        assertSuccessResponse(bpJsDebugger.nextSync());

        onStateChangedQueue.take();

        GetSyncSnapshotsResponse getSyncSnapshotsResponse = bpJsDebugger.getSyncSnapshotsHistory();
        assertNotNull(getSyncSnapshotsResponse);
        SortedMap<Long, BPDebuggerState> syncSnapshotsHistory = getSyncSnapshotsResponse.getSyncSnapShotsHistory();
        assertNotNull(syncSnapshotsHistory);
        assertFalse(syncSnapshotsHistory.isEmpty());

        //todo: assert actual values

        assertSuccessResponse(bpJsDebugger.stop());
        sleepUntil(e -> !bpJsDebugger.isStarted(), 3);
        assertFalse(bpJsDebugger.isStarted());
    }

    private void sleepUntil(Predicate sleepUntil, int maxToTest) throws InterruptedException {
        int counter = 0;
        while (!sleepUntil.test(null) && counter < maxToTest) {
            Thread.sleep(1000);
            counter++;
        }
    }

    @Test
    public void debuggerCommands_withoutSetup() {
        assertErrorResponse(bpJsDebugger.setBreakpoint(1, true), ErrorCode.SETUP_REQUIRED);
        assertErrorResponse(bpJsDebugger.stop(), ErrorCode.SETUP_REQUIRED);
        assertErrorResponse(bpJsDebugger.stepOut(), ErrorCode.SETUP_REQUIRED);
        assertErrorResponse(bpJsDebugger.stepInto(), ErrorCode.SETUP_REQUIRED);
        assertErrorResponse(bpJsDebugger.stepOver(), ErrorCode.SETUP_REQUIRED);
        assertErrorResponse(bpJsDebugger.continueRun(), ErrorCode.SETUP_REQUIRED);
        assertErrorResponse(bpJsDebugger.getState(), ErrorCode.SETUP_REQUIRED);
        assertErrorResponse(bpJsDebugger.getState(), ErrorCode.SETUP_REQUIRED);
        assertErrorResponse(bpJsDebugger.getState(), ErrorCode.SETUP_REQUIRED);
        assertErrorResponse(bpJsDebugger.getState(), ErrorCode.SETUP_REQUIRED);
    }

    @Test
    public void debuggerCommands_afterSetup() {
        setupDebugger();

        when(debuggerEngine.isBreakpointAllowed(anyInt())).thenReturn(false);
        assertErrorResponse(bpJsDebugger.setBreakpoint(1, true), ErrorCode.BREAKPOINT_NOT_ALLOWED);

        assertSuccessResponse(bpJsDebugger.toggleMuteBreakpoints(true));
        verify(debuggerEngine, times(1)).toggleMuteBreakpoints(eq(true));

        when(debuggerEngine.isBreakpointAllowed(anyInt())).thenReturn(true);
        assertSuccessResponse(bpJsDebugger.setBreakpoint(1, true));
        verify(debuggerEngine, times(1)).setBreakpoint(eq(1), eq(true));

        assertErrorResponse(bpJsDebugger.stepOut(), ErrorCode.NOT_IN_JS_DEBUG_STATE);
        verify(debuggerEngine, times(0)).stepOut();

        assertErrorResponse(bpJsDebugger.stepInto(), ErrorCode.NOT_IN_JS_DEBUG_STATE);
        verify(debuggerEngine, times(0)).stepInto();

        assertErrorResponse(bpJsDebugger.stepOver(), ErrorCode.NOT_IN_JS_DEBUG_STATE);
        verify(debuggerEngine, times(0)).stepOver();

        assertErrorResponse(bpJsDebugger.continueRun(), ErrorCode.NOT_IN_JS_DEBUG_STATE);
        verify(debuggerEngine, times(0)).continueRun();
    }

    @Test
    public void debuggerCommands_jsDebugState() throws Exception {
        setupDebugger();
        setDebuggerState(RunnerState.State.JS_DEBUG);

        assertSuccessResponse(bpJsDebugger.stepOut());
        verify(debuggerEngine, times(1)).addCommand(isA(StepOut.class));

        assertSuccessResponse(bpJsDebugger.stepInto());
        verify(debuggerEngine, times(1)).addCommand(isA(StepInto.class));

        assertSuccessResponse(bpJsDebugger.stepOver());
        verify(debuggerEngine, times(1)).addCommand(isA(StepOver.class));

        assertSuccessResponse(bpJsDebugger.continueRun());
        verify(debuggerEngine, times(1)).addCommand(isA(Continue.class));

        assertSuccessResponse(bpJsDebugger.stop());
        verify(debuggerEngine, times(1)).stop();

        assertSuccessResponse(bpJsDebugger.getState());
        verify(debuggerEngine, times(1)).getState();
    }

    @Test
    public void addExternalEvent_jsDebugState() {
        setDebuggerState(RunnerState.State.JS_DEBUG);
        assertErrorResponse(bpJsDebugger.addExternalEvent("externalEvent1"), ErrorCode.CANNOT_ADD_EXTERNAL_EVENT_ON_JS_DEBUG_STATE);
    }

    @Test
    public void addExternalEvent_ableToInjectExternalEvent() {
        setDebuggerState(RunnerState.State.WAITING_FOR_EXTERNAL_EVENT);
        assertSuccessResponse(bpJsDebugger.addExternalEvent("externalEvent1"));
    }

    private void setDebuggerState(RunnerState.State state) {
        bpJsDebugger.getDebuggerState().setDebuggerState(state);
    }

    private void assertSuccessResponse(BooleanResponse booleanResponse) {
        assertTrue(!booleanResponse.isSuccess() ? booleanResponse.getErrorCode().toString() : "", booleanResponse.isSuccess());
        assertNull(booleanResponse.getErrorCode());
    }

    private void assertErrorResponse(BooleanResponse booleanResponse, ErrorCode expectedErrorCode) {
        assertFalse(booleanResponse.isSuccess());
        assertEquals(expectedErrorCode, booleanResponse.getErrorCode());
    }

    private static Void onStateChangedTester(BPDebuggerState debuggerState) {
        onStateChangedQueue.add(debuggerState);
        return null;
    }
}