package il.ac.bgu.se.bp.execution;

import il.ac.bgu.cs.bp.bpjs.BPjs;
import il.ac.bgu.cs.bp.bpjs.bprogramio.BProgramSyncSnapshotIO;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.BProgramRunnerListener;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.PrintBProgramRunnerListener;
import il.ac.bgu.cs.bp.bpjs.model.*;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.EventSelectionResult;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.EventSelectionStrategy;
import il.ac.bgu.se.bp.debugger.BPJsDebugger;
import il.ac.bgu.se.bp.debugger.DebuggerLevel;
import il.ac.bgu.se.bp.debugger.RunnerState;
import il.ac.bgu.se.bp.debugger.commands.*;
import il.ac.bgu.se.bp.debugger.engine.DebuggerEngine;
import il.ac.bgu.se.bp.debugger.engine.DebuggerEngineImpl;
import il.ac.bgu.se.bp.debugger.engine.SyncSnapshotHolder;
import il.ac.bgu.se.bp.debugger.engine.SyncSnapshotHolderImpl;
import il.ac.bgu.se.bp.debugger.engine.events.BPConsoleEvent;
import il.ac.bgu.se.bp.debugger.engine.events.ProgramStatusEvent;
import il.ac.bgu.se.bp.debugger.manage.ProgramValidator;
import il.ac.bgu.se.bp.error.ErrorCode;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.DebugResponse;
import il.ac.bgu.se.bp.rest.response.GetSyncSnapshotsResponse;
import il.ac.bgu.se.bp.rest.response.StepResponse;
import il.ac.bgu.se.bp.rest.response.SyncSnapshot;
import il.ac.bgu.se.bp.socket.console.ConsoleMessage;
import il.ac.bgu.se.bp.socket.console.LogType;
import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import il.ac.bgu.se.bp.socket.state.BThreadInfo;
import il.ac.bgu.se.bp.socket.state.BThreadScope;
import il.ac.bgu.se.bp.socket.state.EventInfo;
import il.ac.bgu.se.bp.socket.status.Status;
import il.ac.bgu.se.bp.utils.DebuggerBProgramRunnerListener;
import il.ac.bgu.se.bp.utils.DebuggerExecutorServiceMaker;
import il.ac.bgu.se.bp.utils.DebuggerPrintStream;
import il.ac.bgu.se.bp.utils.DebuggerStateHelper;
import il.ac.bgu.se.bp.utils.logger.Logger;
import il.ac.bgu.se.bp.utils.observer.BPEvent;
import il.ac.bgu.se.bp.utils.observer.Subscriber;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static il.ac.bgu.cs.bp.bpjs.model.StorageModificationStrategy.PASSTHROUGH;
import static il.ac.bgu.se.bp.utils.Common.NO_MORE_WAIT_EXTERNAL;
import static il.ac.bgu.se.bp.utils.ProgramStatusHelper.getRunStatusByDebuggerLevel;
import static il.ac.bgu.se.bp.utils.ResponseHelper.createErrorResponse;
import static il.ac.bgu.se.bp.utils.ResponseHelper.createSuccessResponse;
import static java.util.Collections.reverseOrder;

/**
 * COBP Debugger Implementation that extends the standard BPJs debugger
 * to support Context-Oriented Behavior Programming (COBP).
 * 
 * This implementation uses DebugableContextBProgram instead of ResourceBProgram
 * to provide COBP functionality while maintaining full debugging capabilities.
 */
public class COBPDebuggerImpl implements BPJsDebugger<BooleanResponse> {
    private final static AtomicInteger debuggerThreadIdGenerator = new AtomicInteger(0);
    private Logger logger;

    private String debuggerId;
    private String filename;
    private String debuggerExecutorId;

    private volatile boolean isBProgSetup = false; //indicates if bprog after setup
    private volatile boolean isSetup = false;
    private volatile boolean isStarted = false;
    private volatile boolean isSkipSyncPoints = false;

    private ExecutorService jsExecutorService;
    private ExecutorService bpExecutorService;
    private BProgram bprog;
    private DebuggerEngine<BProgramSyncSnapshot> debuggerEngine;
    private BProgramSyncSnapshot syncSnapshot;
    private int numOfLines;

    private final RunnerState state = new RunnerState();
    private final DebuggerLevel debuggerLevel;
    private final SyncSnapshotHolder<BProgramSyncSnapshot, BEvent> syncSnapshotHolder = new SyncSnapshotHolderImpl();
    private final DebuggerStateHelper debuggerStateHelper;
    private final DebuggerPrintStream debuggerPrintStream = new DebuggerPrintStream();
    private final List<BProgramRunnerListener> listeners = new ArrayList<>();
    private final List<Subscriber<BPEvent>> subscribers = new ArrayList<>();

    @Autowired
    private ProgramValidator<BPJsDebugger> bPjsProgramValidator;

    public COBPDebuggerImpl(String debuggerId, String filename, DebuggerLevel debuggerLevel) {
        this.debuggerId = debuggerId;
        this.filename = filename;
        this.debuggerLevel = debuggerLevel;
        debuggerStateHelper = new DebuggerStateHelper(this, syncSnapshotHolder, debuggerLevel);
        BPjs.setExecutorServiceMaker(new DebuggerExecutorServiceMaker());
        initDebugger();
    }

    public COBPDebuggerImpl(String debuggerId, String filename) {
        this(debuggerId, filename, DebuggerLevel.NORMAL);
    }

    private void initDebugger() {
        debuggerExecutorId = "COBPDebuggerRunner-" + debuggerThreadIdGenerator.incrementAndGet();
        jsExecutorService = BPjs.getExecutorServiceMaker().makeWithName(debuggerExecutorId);
        bpExecutorService = BPjs.getExecutorServiceMaker().makeWithName(debuggerExecutorId);
        logger = new Logger(COBPDebuggerImpl.class, debuggerId);
        debuggerEngine = new DebuggerEngineImpl(debuggerId, filename, state, debuggerStateHelper, debuggerExecutorId);
        debuggerEngine.changeDebuggerLevel(debuggerLevel);
        debuggerPrintStream.setDebuggerId(debuggerId);
        
        // Use DebugableContextBProgram instead of ResourceBProgram for COBP support
        bprog = new il.ac.bgu.cs.bp.bpjs.context.DebugableContextBProgram(filename);
        initListeners(bprog);
    }

    private void initListeners(BProgram bProgram) {
        listeners.add(new PrintBProgramRunnerListener(debuggerPrintStream));
        listeners.add(new DebuggerBProgramRunnerListener(debuggerStateHelper));
        bProgram.setAddBThreadCallback((bp, bt) -> listeners.forEach(l -> l.bthreadAdded(bp, bt)));
    }

    @Override
    public DebugResponse setup(Map<Integer, Boolean> breakpoints, boolean isSkipBreakpoints, boolean isSkipSyncPoints, boolean isWaitForExternalEvents) {
        logger.info("COBP setup isSkipBreakpoints: {0}, isSkipSyncPoints: {1}, isWaitForExternalEvents: {2}", isSkipSyncPoints, isSkipBreakpoints, isWaitForExternalEvents);
        if (!isBProgSetup) { // may get twice to setup - must do bprog setup first time only
            listeners.forEach(l -> l.starting(bprog));
            bprog.setLoggerOutputStreamer(debuggerPrintStream);
            syncSnapshot = awaitForExecutorServiceToFinishTask(bprog::setup);
            if (syncSnapshot == null) {
                onExit();
                return new DebugResponse(false, ErrorCode.BP_SETUP_FAIL, new boolean[0]);
            }
            syncSnapshot.getBThreadSnapshots().forEach(sn -> listeners.forEach(l -> l.bthreadAdded(bprog, sn)));
            isBProgSetup = true;
            SafetyViolationTag violationTag = syncSnapshot.getViolationTag();
            if (violationTag != null && !StringUtils.isEmpty(violationTag.getMessage())) {
                onExit();
                return new DebugResponse(false, ErrorCode.BP_SETUP_FAIL, new boolean[0]);
            }
        }
        toggleMuteSyncPoints(isSkipSyncPoints);
        debuggerEngine.setupBreakpoints(breakpoints);
        debuggerEngine.toggleMuteBreakpoints(isSkipBreakpoints);

        debuggerEngine.setSyncSnapshot(syncSnapshot);
        afterSetup();
        state.setDebuggerState(RunnerState.State.STOPPED);
        boolean[] actualBreakpoints = debuggerEngine.getBreakpoints();
        numOfLines = actualBreakpoints.length;

        bprog.setWaitForExternalEvents(isWaitForExternalEvents);
        return new DebugResponse(true, actualBreakpoints);
    }

    @Override
    public synchronized BooleanResponse toggleMuteSyncPoints(boolean toggleMuteSyncPoints) {
        logger.info("COBP toggleMuteSyncPoints to: {0}", toggleMuteSyncPoints);
        this.isSkipSyncPoints = toggleMuteSyncPoints;
        return createSuccessResponse();
    }

    @Override
    public GetSyncSnapshotsResponse getSyncSnapshotsHistory() {
        SortedMap<Long, BPDebuggerState> syncSnapshotsHistory = new TreeMap<>();

        syncSnapshotHolder.getAllSyncSnapshots().forEach((time, bProgramSyncSnapshotBEventPair) ->
                syncSnapshotsHistory.put(time, debuggerStateHelper
                        .generateDebuggerState(bProgramSyncSnapshotBEventPair.getLeft(), state, null, null)));

        return new GetSyncSnapshotsResponse(syncSnapshotsHistory);
    }

    @Override
    public byte[] getSyncSnapshot() {
        try {
            return new BProgramSyncSnapshotIO(bprog).serialize(syncSnapshot);
        } catch (Exception e) {
            logger.error("failed serializing COBP bprog SyncSnapshot", e);
            return null;
        }
    }

    @Override
    public BooleanResponse setSyncSnapshot(long snapShotTime) {
        logger.info("COBP setSyncSnapshot() snapShotTime: {0}, state: {1}", snapShotTime, state.getDebuggerState().toString());
        if (!checkStateEquals(RunnerState.State.SYNC_STATE)) {
            return createErrorResponse(ErrorCode.NOT_IN_BP_SYNC_STATE);
        }
        BProgramSyncSnapshot newSnapshot = syncSnapshotHolder.popKey(snapShotTime);
        if (newSnapshot == null) {
            return createErrorResponse(ErrorCode.CANNOT_REPLACE_SNAPSHOT);
        }

        return setSyncSnapshot(newSnapshot);
    }

    @Override
    public BooleanResponse setSyncSnapshot(SyncSnapshot syncSnapshotHolder) {
        try {
            return setSyncSnapshot(new BProgramSyncSnapshotIO(bprog).deserialize(syncSnapshotHolder.getSyncSnapshot()));
        } catch (Exception e) {
            logger.error("deserialization from sync snapshot bytes to object failed", e);
            return createErrorResponse(ErrorCode.IMPORT_SYNC_SNAPSHOT_FAILURE);
        }
    }

    private BooleanResponse setSyncSnapshot(BProgramSyncSnapshot newSnapshot) {
        syncSnapshot = newSnapshot;
        debuggerStateHelper.cleanFields();
        debuggerEngine.setSyncSnapshot(syncSnapshot);
        debuggerEngine.onStateChanged();
        return createSuccessResponse();
    }

    @Override
    public RunnerState getDebuggerState() {
        return state;
    }

    @Override
    public String getDebuggerExecutorId() {
        return debuggerExecutorId;
    }

    @Override
    public SortedMap<Long, EventInfo> getEventsHistory(int from, int to) {
        if (from < 0 || to < 0 || to < from) {
            return null;
        }
        return debuggerStateHelper.generateEventsHistory(from, to);
    }

    private synchronized void setIsStarted(boolean isStarted) {
        this.isStarted = isStarted;
    }

    private synchronized void afterSetup() {
        this.isSetup = true;
    }

    @Override
    public synchronized boolean isSetup() {
        return isSetup;
    }

    @Override
    public synchronized boolean isStarted() {
        return isStarted;
    }

    @Override
    public DebugResponse startSync(Map<Integer, Boolean> breakpointsMap, boolean isSkipSyncPoints, boolean isSkipBreakpoints, boolean isWaitForExternalEvents) {
        notifySubscribers(new ProgramStatusEvent(debuggerId, getRunStatusByDebuggerLevel(debuggerLevel)));
        DebugResponse debugResponse = setup(breakpointsMap, isSkipBreakpoints, isSkipSyncPoints, isWaitForExternalEvents);
        if (debugResponse.isSuccess()) {
            bpExecutorService.execute(this::runStartSync);
        }
        return debugResponse;
    }

    private void runStartSync() {
        try {
            setIsStarted(true);
            listeners.forEach(l -> l.started(bprog));
            syncSnapshot = syncSnapshot.start(jsExecutorService, PASSTHROUGH);
            if (!syncSnapshot.isStateValid()) {
                onInvalidStateError("COBP Start sync fatal error");
                onExit();
                return;
            }
            state.setDebuggerState(RunnerState.State.SYNC_STATE);
            debuggerEngine.setSyncSnapshot(syncSnapshot);
            syncSnapshotHolder.addSyncSnapshot(syncSnapshot, null);
            logger.info("~COBP FIRST SYNC STATE~");
            if (isSkipSyncPoints) {
                nextSync();
            }
            else {
                logger.debug("Generate state from COBP startSync");
                debuggerEngine.onStateChanged();
                notifySubscribers(new ProgramStatusEvent(debuggerId, Status.SYNCSTATE));
            }
        } catch (InterruptedException e) {
            if (debuggerEngine.isRunning()) {
                logger.warning("got InterruptedException in COBP startSync");
                onExit();
            }
        } catch (RejectedExecutionException e) {
            logger.error("Forced to stop COBP debugger");
            onExit();
        } catch (Exception e) {
            logger.error("COBP runStartSync failed, error: {0}", e.getMessage());
            notifySubscribers(new BPConsoleEvent(debuggerId, new ConsoleMessage(e.getMessage(), LogType.error)));
        }
    }

    private void onInvalidStateError(String error) {
        SafetyViolationTag violationTag = syncSnapshot.getViolationTag();
        listeners.forEach(l -> l.assertionFailed(bprog, violationTag));
        state.setDebuggerState(RunnerState.State.STOPPED);
        logger.error(error);
    }

    @Override
    public BooleanResponse nextSync() {
        BooleanResponse booleanResponse = bPjsProgramValidator.validateNextSync(this);
        if (!booleanResponse.isSuccess()) {
            return booleanResponse;
        }

        if (!syncSnapshot.isStateValid()) {
            onInvalidStateError("COBP next sync fatal error");
            return createErrorResponse(ErrorCode.INVALID_SYNC_SNAPSHOT_STATE);
        }

        bpExecutorService.execute(this::runNextSync);
        return createSuccessResponse();
    }
    
    public StepResponse nextSyncWithStepResponse() {
        BooleanResponse validation = bPjsProgramValidator.validateNextSync(this);
        if (!validation.isSuccess()) {
            return new StepResponse(false, validation.getErrorCode().toString(), null);
        }

        if (!syncSnapshot.isStateValid()) {
            onInvalidStateError("COBP next sync fatal error");
            return new StepResponse(false, ErrorCode.INVALID_SYNC_SNAPSHOT_STATE.toString(), null);
        }

        bpExecutorService.execute(this::runNextSync);
        
        // Try to get the real debugger state after nextSync
        try {
            BPDebuggerState debuggerState = debuggerStateHelper.generateDebuggerState(
                syncSnapshot, state, null, null);
            BPDebuggerState safeState = createSerializationSafeState(debuggerState);
            return new StepResponse(true, null, safeState);
        } catch (Exception e) {
            logger.error("nextSyncWithStepResponse - Failed to generate debugger state: {0}", e.getMessage());
            return new StepResponse(false, "SERIALIZATION_ERROR: " + e.getMessage(), null);
        }
    }

    private void runNextSync() {
        logger.info("COBP runNextSync state: {0}", state.getDebuggerState());
        if (!isThereAnyPossibleEvents()) {
            if (!bprog.isWaitForExternalEvents()) {
                debuggerEngine.onStateChanged();
                logger.info("COBP Event queue empty, not need to wait to external event. terminating....");
                listeners.forEach(l -> l.ended(bprog));
                onExit();
                listeners.forEach(l -> l.superstepDone(bprog));
                return;
            }
            nextSyncOnNoPossibleEvents();
        }
        EventSelectionStrategy eventSelectionStrategy = bprog.getEventSelectionStrategy();
        Set<BEvent> possibleEvents = eventSelectionStrategy.selectableEvents(syncSnapshot);
        if (possibleEvents.isEmpty()) {
            runNextSync();
            return;
        }

        state.setDebuggerState(RunnerState.State.RUNNING);
        notifySubscribers(new ProgramStatusEvent(debuggerId, getRunStatusByDebuggerLevel(debuggerLevel)));

        logger.info("COBP External events: {0}, possibleEvents: {1}", syncSnapshot.getExternalEvents(), possibleEvents);

        try {
            Optional<EventSelectionResult> eventOptional = eventSelectionStrategy.select(syncSnapshot, possibleEvents);
            if (eventOptional.isPresent()) {
                nextSyncOnChosenEvent(eventOptional.get());
            }
            else {
                logger.info("COBP Events queue is empty");
            }
        } catch (InterruptedException e) {
            if (debuggerEngine.isRunning()) {
                logger.error("COBP runNextSync: got InterruptedException in nextSync");
            }
        } catch (Exception e) {
            logger.error("COBP runNextSync failed, error: {0}", e.getMessage());
            notifySubscribers(new BPConsoleEvent(debuggerId, new ConsoleMessage(e.getMessage(), LogType.error)));
        }
    }

    private boolean isThereAnyPossibleEvents() {
        EventSelectionStrategy eventSelectionStrategy = bprog.getEventSelectionStrategy();
        Set<BEvent> possibleEvents = eventSelectionStrategy.selectableEvents(syncSnapshot);
        return !possibleEvents.isEmpty();
    }

    private void nextSyncOnChosenEvent(EventSelectionResult eventSelectionResult) throws Exception {
        BEvent event = eventSelectionResult.getEvent();
        if (!eventSelectionResult.getIndicesToRemove().isEmpty()) {
            removeExternalEvents(eventSelectionResult);
        }
        logger.info("COBP Triggering event " + event);
        debuggerStateHelper.updateCurrentEvent(event.getName());
        BProgramSyncSnapshot lastSnapshot = syncSnapshot;
        debuggerEngine.setSyncSnapshot(syncSnapshot);
        syncSnapshot = syncSnapshot.triggerEvent(event, jsExecutorService, listeners, PASSTHROUGH);
        if (!syncSnapshot.isStateValid()) {
            onInvalidStateError("COBP Next Sync fatal error");
            return;
        }
        state.setDebuggerState(RunnerState.State.SYNC_STATE);
        if (!event.equals(NO_MORE_WAIT_EXTERNAL)) {
            syncSnapshotHolder.addSyncSnapshot(lastSnapshot, event);
        }
        debuggerEngine.setSyncSnapshot(syncSnapshot);
        logger.info("~COBP NEW SYNC STATE~");
        if (isSkipSyncPoints || event.equals(NO_MORE_WAIT_EXTERNAL)) {
            nextSync();
        }
        else {
            logger.debug("Generate state from COBP nextSync");
            notifySubscribers(new ProgramStatusEvent(debuggerId, Status.SYNCSTATE));
            debuggerEngine.onStateChanged();
        }
    }

    private void nextSyncOnNoPossibleEvents() {
        debuggerEngine.onStateChanged();
        logger.info("COBP waiting for external event");
        state.setDebuggerState(RunnerState.State.WAITING_FOR_EXTERNAL_EVENT);
        try {
            notifySubscribers(new ProgramStatusEvent(debuggerId, Status.WAITING_FOR_EXTERNAL_EVENT));
            BEvent next = bprog.takeExternalEvent(); // and now we wait for external event
            if (next == null) {
                logger.info("COBP Event queue empty, not need to wait to external event. terminating....");
                listeners.forEach(l -> l.ended(bprog));
                onExit();
            }
            else {
                syncSnapshot.getExternalEvents().add(next);
            }
        } catch (Exception e) {
            logger.error("COBP nextSyncOnNoPossibleEvents error: {0}", e.getMessage());
        }
    }

    private void onExit() {
        logger.info("started COBP onExit process");
        debuggerEngine.stop();
        jsExecutorService.shutdownNow();
        bpExecutorService.shutdownNow();
        sleep();
        if (!jsExecutorService.isTerminated()) {
            forceStopDebugger();
        }
    }

    private void forceStopDebugger() {
        logger.info("COBP debugger is still running, trying to force stop");
        isSkipSyncPoints = false;
        debuggerEngine.toggleMuteBreakpoints(false);

        for (int i = 0; i < numOfLines; i++) {
            if (debuggerEngine.isBreakpointAllowed(i)) {
                new SetBreakpoint(i, true).applyCommand(debuggerEngine);
            }
        }
    }

    private void sleep() {
        try {
            logger.info("COBP sleeping " + 1 + " sec");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    private void removeExternalEvents(EventSelectionResult esr) {
        // the event selection affected the external event queue.
        List<BEvent> updatedExternals = new ArrayList<>(syncSnapshot.getExternalEvents());
        esr.getIndicesToRemove().stream().sorted(reverseOrder())
                .forEach(idxObj -> updatedExternals.remove(idxObj.intValue()));
        syncSnapshot = syncSnapshot.copyWith(updatedExternals);
        debuggerEngine.setSyncSnapshot(syncSnapshot);
    }

    private <T> T awaitForExecutorServiceToFinishTask(Callable<T> callable) {
        try {
            return jsExecutorService.submit(callable).get();
        } catch (Exception e) {
            logger.error("failed running callable task via executor service, error: {0}", e, e.getMessage());
            notifySubscribers(new BPConsoleEvent(debuggerId, new ConsoleMessage(e.getMessage(), LogType.error)));
        }
        return null;
    }

    @Override
    public BooleanResponse continueRun() {
        return bPjsProgramValidator.validateAndRunAsync(this, RunnerState.State.JS_DEBUG,
                createAddCommandCallback(new Continue()));
    }

    @Override
    public BooleanResponse stepInto() {
        return bPjsProgramValidator.validateAndRun(this, RunnerState.State.JS_DEBUG,
                createAddCommandCallback(new StepInto()));
    }
    
    public StepResponse stepIntoWithStepResponse() {
        BooleanResponse validation = bPjsProgramValidator.validateAndRun(this, RunnerState.State.JS_DEBUG,
                createAddCommandCallback(new StepInto()));
        
        if (!validation.isSuccess()) {
            return new StepResponse(false, validation.getErrorCode().toString(), null);
        }
        
        // Try to get the real debugger state
        try {
            BPDebuggerState debuggerState = debuggerStateHelper.generateDebuggerState(
                syncSnapshot, state, null, null);
            BPDebuggerState safeState = createSerializationSafeState(debuggerState);
            return new StepResponse(true, null, safeState);
        } catch (Exception e) {
            logger.error("stepIntoWithStepResponse - Failed to generate debugger state: {0}", e.getMessage());
            return new StepResponse(false, "SERIALIZATION_ERROR: " + e.getMessage(), null);
        }
    }

    @Override
    public BooleanResponse stepOver() {
        return bPjsProgramValidator.validateAndRun(this, RunnerState.State.JS_DEBUG,
                createAddCommandCallback(new StepOver()));
    }
    
    public StepResponse stepOverWithStepResponse() {
        BooleanResponse validation = bPjsProgramValidator.validateAndRun(this, RunnerState.State.JS_DEBUG,
                createAddCommandCallback(new StepOver()));
        
        if (!validation.isSuccess()) {
            return new StepResponse(false, validation.getErrorCode().toString(), null);
        }
        
        // Try to get the real debugger state
        try {
            BPDebuggerState debuggerState = debuggerStateHelper.generateDebuggerState(
                syncSnapshot, state, null, null);
            BPDebuggerState safeState = createSerializationSafeState(debuggerState);
            return new StepResponse(true, null, safeState);
        } catch (Exception e) {
            logger.error("stepOverWithStepResponse - Failed to generate debugger state: {0}", e.getMessage());
            return new StepResponse(false, "SERIALIZATION_ERROR: " + e.getMessage(), null);
        }
    }

    @Override
    public BooleanResponse stepOut() {
        return bPjsProgramValidator.validateAndRun(this, RunnerState.State.JS_DEBUG,
                createAddCommandCallback(new StepOut()));
    }
    
    public StepResponse stepOutWithStepResponse() {
        BooleanResponse validation = bPjsProgramValidator.validateAndRun(this, RunnerState.State.JS_DEBUG,
                createAddCommandCallback(new StepOut()));
        
        if (!validation.isSuccess()) {
            return new StepResponse(false, validation.getErrorCode().toString(), null);
        }
        
        // Try to get the real debugger state
        try {
            BPDebuggerState debuggerState = debuggerStateHelper.generateDebuggerState(
                syncSnapshot, state, null, null);
            BPDebuggerState safeState = createSerializationSafeState(debuggerState);
            return new StepResponse(true, null, safeState);
        } catch (Exception e) {
            logger.error("stepOutWithStepResponse - Failed to generate debugger state: {0}", e.getMessage());
            return new StepResponse(false, "SERIALIZATION_ERROR: " + e.getMessage(), null);
        }
    }

    // New methods that return StepResponse with debugger state
    public StepResponse stepIntoWithState() {
        // Debug logging
        logger.info("stepIntoWithState called - Current state: {0}", state.getDebuggerState());
        
        // For COBP, we need to validate SYNC_STATE or STOPPED state
        // STOPPED state is acceptable if the program failed to start but we still want to show state
        if (!checkStateEquals(RunnerState.State.SYNC_STATE) && !checkStateEquals(RunnerState.State.STOPPED)) {
            logger.info("stepIntoWithState - State validation failed. Current: {0}, Expected: SYNC_STATE or STOPPED", state.getDebuggerState());
            return new StepResponse(false, ErrorCode.NOT_IN_BP_SYNC_STATE.toString(), null);
        }
        
        // Try to get the real debugger state first, with fallback to minimal state
        try {
            logger.info("stepIntoWithState - Attempting to generate real debugger state");
            BPDebuggerState debuggerState = debuggerStateHelper.generateDebuggerState(
                syncSnapshot, state, null, null);
            
            // Try to create a serialization-safe copy
            BPDebuggerState safeState = createSerializationSafeState(debuggerState);
            logger.info("stepIntoWithState - Successfully generated real debugger state with {0} b-threads", 
                safeState.getbThreadInfoList() != null ? safeState.getbThreadInfoList().size() : 0);
            return new StepResponse(true, null, safeState);
            
        } catch (Exception e) {
            logger.error("stepIntoWithState - Failed to generate real debugger state: {0}", e.getMessage());
            // Return error response instead of fake minimal state
            return new StepResponse(false, "SERIALIZATION_ERROR: " + e.getMessage(), null);
        }
    }

    public StepResponse stepOverWithState() {
        // For COBP, we need to validate SYNC_STATE or STOPPED state
        // STOPPED state is acceptable if the program failed to start but we still want to show state
        if (!checkStateEquals(RunnerState.State.SYNC_STATE) && !checkStateEquals(RunnerState.State.STOPPED)) {
            return new StepResponse(false, ErrorCode.NOT_IN_BP_SYNC_STATE.toString(), null);
        }
        
        // Try to get the real debugger state first, with fallback to minimal state
        try {
            logger.info("stepOverWithState - Attempting to generate real debugger state");
            BPDebuggerState debuggerState = debuggerStateHelper.generateDebuggerState(
                syncSnapshot, state, null, null);
            
            // Try to create a serialization-safe copy
            BPDebuggerState safeState = createSerializationSafeState(debuggerState);
            logger.info("stepOverWithState - Successfully generated real debugger state with {0} b-threads", 
                safeState.getbThreadInfoList() != null ? safeState.getbThreadInfoList().size() : 0);
            return new StepResponse(true, null, safeState);
            
        } catch (Exception e) {
            logger.error("stepOverWithState - Failed to generate real debugger state: {0}", e.getMessage());
            // Return error response instead of fake minimal state
            return new StepResponse(false, "SERIALIZATION_ERROR: " + e.getMessage(), null);
        }
    }

    public StepResponse stepOutWithState() {
        // For COBP, we need to validate SYNC_STATE or STOPPED state
        // STOPPED state is acceptable if the program failed to start but we still want to show state
        if (!checkStateEquals(RunnerState.State.SYNC_STATE) && !checkStateEquals(RunnerState.State.STOPPED)) {
            return new StepResponse(false, ErrorCode.NOT_IN_BP_SYNC_STATE.toString(), null);
        }
        
        // Try to get the real debugger state first, with fallback to minimal state
        try {
            logger.info("stepOutWithState - Attempting to generate real debugger state");
            BPDebuggerState debuggerState = debuggerStateHelper.generateDebuggerState(
                syncSnapshot, state, null, null);
            
            // Try to create a serialization-safe copy
            BPDebuggerState safeState = createSerializationSafeState(debuggerState);
            logger.info("stepOutWithState - Successfully generated real debugger state with {0} b-threads", 
                safeState.getbThreadInfoList() != null ? safeState.getbThreadInfoList().size() : 0);
            return new StepResponse(true, null, safeState);
            
        } catch (Exception e) {
            logger.error("stepOutWithState - Failed to generate real debugger state: {0}", e.getMessage());
            // Return error response instead of fake minimal state
            return new StepResponse(false, "SERIALIZATION_ERROR: " + e.getMessage(), null);
        }
    }

    public StepResponse nextSyncWithState() {
        // For COBP, we need to validate SYNC_STATE or STOPPED state
        // STOPPED state is acceptable if the program failed to start but we still want to show state
        if (!checkStateEquals(RunnerState.State.SYNC_STATE) && !checkStateEquals(RunnerState.State.STOPPED)) {
            return new StepResponse(false, ErrorCode.NOT_IN_BP_SYNC_STATE.toString(), null);
        }
        
        // Try to get the real debugger state first, with fallback to minimal state
        try {
            logger.info("nextSyncWithState - Attempting to generate real debugger state");
            BPDebuggerState debuggerState = debuggerStateHelper.generateDebuggerState(
                syncSnapshot, state, null, null);
            
            // Try to create a serialization-safe copy
            BPDebuggerState safeState = createSerializationSafeState(debuggerState);
            logger.info("nextSyncWithState - Successfully generated real debugger state with {0} b-threads", 
                safeState.getbThreadInfoList() != null ? safeState.getbThreadInfoList().size() : 0);
            return new StepResponse(true, null, safeState);
            
        } catch (Exception e) {
            logger.error("nextSyncWithState - Failed to generate real debugger state: {0}", e.getMessage());
            // Return error response instead of fake minimal state
            return new StepResponse(false, "SERIALIZATION_ERROR: " + e.getMessage(), null);
        }
    }

    private BPDebuggerState createSerializationSafeState(BPDebuggerState originalState) {
        if (originalState == null) {
            return new BPDebuggerState();
        }
        
        // Create a new state object to avoid cyclic references
        BPDebuggerState safeState = new BPDebuggerState();
        
        // Copy basic fields safely
        safeState.setCurrentRunningBT(originalState.getCurrentRunningBT());
        safeState.setCurrentLineNumber(originalState.getCurrentLineNumber());
        
        // Copy b-thread info list safely (avoiding cyclic references)
        if (originalState.getbThreadInfoList() != null) {
            List<BThreadInfo> safeBThreads = new ArrayList<>();
            for (BThreadInfo bThread : originalState.getbThreadInfoList()) {
                if (bThread != null) {
                    // Create a safe copy of BThreadInfo
                    BThreadInfo safeBThread = new BThreadInfo();
                    safeBThread.setName(bThread.getName());
                    
                    // Copy environment safely
                    if (bThread.getEnv() != null) {
                        Map<Integer, BThreadScope> safeEnv = new HashMap<>();
                        for (Map.Entry<Integer, BThreadScope> entry : bThread.getEnv().entrySet()) {
                            if (entry.getValue() != null) {
                                BThreadScope safeScope = new BThreadScope();
                                safeScope.setScopeName(entry.getValue().getScopeName());
                                safeScope.setCurrentLineNumber(entry.getValue().getCurrentLineNumber());
                                
                                // Copy variables safely (avoiding complex objects)
                                if (entry.getValue().getVariables() != null) {
                                    Map<String, String> safeVariables = new HashMap<>();
                                    for (Map.Entry<String, String> varEntry : entry.getValue().getVariables().entrySet()) {
                                        safeVariables.put(varEntry.getKey(), varEntry.getValue());
                                    }
                                    safeScope.setVariables(safeVariables);
                                }
                                safeEnv.put(entry.getKey(), safeScope);
                            }
                        }
                        safeBThread.setEnv(safeEnv);
                    }
                    
                    // Copy event sets safely
                    if (bThread.getRequested() != null) {
                        Set<EventInfo> safeRequested = new HashSet<>();
                        for (EventInfo event : bThread.getRequested()) {
                            if (event != null) {
                                safeRequested.add(new EventInfo(event.getName()));
                            }
                        }
                        safeBThread.setRequested(safeRequested);
                    }
                    
                    if (bThread.getBlocked() != null) {
                        Set<EventInfo> safeBlocked = new HashSet<>();
                        for (EventInfo event : bThread.getBlocked()) {
                            if (event != null) {
                                safeBlocked.add(new EventInfo(event.getName()));
                            }
                        }
                        safeBThread.setBlocked(safeBlocked);
                    }
                    
                    if (bThread.getWait() != null) {
                        Set<EventInfo> safeWait = new HashSet<>();
                        for (EventInfo event : bThread.getWait()) {
                            if (event != null) {
                                safeWait.add(new EventInfo(event.getName()));
                            }
                        }
                        safeBThread.setWait(safeWait);
                    }
                    
                    safeBThreads.add(safeBThread);
                }
            }
            safeState.setbThreadInfoList(safeBThreads);
        }
        
        return safeState;
    }



    @Override
    public BooleanResponse setBreakpoint(final int lineNumber, final boolean stopOnBreakpoint) {
        Callable<BooleanResponse> applyCommandCallback = createApplyCommandCallback(new SetBreakpoint(lineNumber, stopOnBreakpoint), debuggerEngine);
        return bPjsProgramValidator.validateAndRun(this, applyCommandCallback);
    }

    @Override
    public BooleanResponse stop() {
        if (!isSetup()) {
            return createErrorResponse(ErrorCode.SETUP_REQUIRED);
        }
        setIsStarted(false);
        onExit();
        notifySubscribers(new ProgramStatusEvent(debuggerId, Status.STOP));
        return createSuccessResponse();
    }

    @Override
    public BooleanResponse getState() {
        Callable<BooleanResponse> applyCommandCallback = createApplyCommandCallback(new GetState(), debuggerEngine);
        return bPjsProgramValidator.validateAndRun(this, applyCommandCallback);
    }

    @Override
    public BooleanResponse toggleMuteBreakpoints(boolean toggleBreakPointStatus) {
        Callable<BooleanResponse> applyCommandCallback = createApplyCommandCallback(new ToggleMuteBreakpoints(toggleBreakPointStatus), debuggerEngine);
        return bPjsProgramValidator.validateAndRun(this, applyCommandCallback);
    }

    @Override
    public BooleanResponse addExternalEvent(String externalEvent) {
        if (StringUtils.isEmpty(externalEvent)) {
            return createErrorResponse(ErrorCode.INVALID_EVENT);
        }

        BooleanResponse booleanResponse = bPjsProgramValidator.validateNotJSDebugState(this);
        if (!booleanResponse.isSuccess()) {
            return booleanResponse;
        }

        logger.info("Adding external event to COBP: {0}, debugger state: {1}", externalEvent, state.getDebuggerState());
        BEvent bEvent = new BEvent(externalEvent);

        if (checkStateEquals(RunnerState.State.WAITING_FOR_EXTERNAL_EVENT)) {
            bprog.enqueueExternalEvent(bEvent);
        }
        else {
            List<BEvent> updatedExternals = new ArrayList<>(syncSnapshot.getExternalEvents());
            updatedExternals.add(bEvent);
            syncSnapshot = syncSnapshot.copyWith(updatedExternals);
            debuggerEngine.setSyncSnapshot(syncSnapshot);
            debuggerEngine.onStateChanged();
        }

        return createSuccessResponse();
    }

    @Override
    public BooleanResponse removeExternalEvent(String externalEvent) {
        if (StringUtils.isEmpty(externalEvent)) {
            return createErrorResponse(ErrorCode.INVALID_EVENT);
        }
        List<BEvent> updatedExternals = new ArrayList<>(syncSnapshot.getExternalEvents());
        updatedExternals.removeIf(bEvent -> bEvent.getName().equals(externalEvent));
        syncSnapshot = syncSnapshot.copyWith(updatedExternals);
        debuggerEngine.setSyncSnapshot(syncSnapshot);
        debuggerEngine.onStateChanged();
        return createSuccessResponse();
    }

    @Override
    public BooleanResponse toggleWaitForExternalEvents(boolean shouldWait) {
        bprog.setWaitForExternalEvents(shouldWait);
        return createSuccessResponse();
    }

    private Callable<BooleanResponse> createAddCommandCallback(DebuggerCommand debuggerCommand) {
        return () -> addCommand(debuggerCommand);
    }

    private Callable<BooleanResponse> createApplyCommandCallback(DebuggerCommand debuggerCommand, DebuggerEngine debugger) {
        return () -> debuggerCommand.applyCommand(debugger);
    }

    private BooleanResponse addCommand(DebuggerCommand debuggerCommand) {
        try {
            debuggerEngine.addCommand(debuggerCommand);
            return createSuccessResponse();
        } catch (Exception e) {
            logger.error("failed adding command: {0}", e, debuggerCommand.toString());
        }
        return createErrorResponse(ErrorCode.FAILED_ADDING_COMMAND);
    }

    @Override
    public void subscribe(Subscriber<BPEvent> subscriber) {
        subscribers.add(subscriber);
        debuggerEngine.subscribe(subscriber);
        debuggerPrintStream.subscribe(subscriber);
    }

    @Override
    public void unsubscribe(Subscriber<BPEvent> subscriber) {
        subscribers.remove(subscriber);
        debuggerEngine.unsubscribe(subscriber);
        debuggerPrintStream.unsubscribe(subscriber);
    }

    @Override
    public void notifySubscribers(BPEvent event) {
        for (Subscriber<BPEvent> subscriber : subscribers) {
            subscriber.update(event);
        }
    }

    @Override
    public boolean isSkipSyncPoints() {
        return isSkipSyncPoints;
    }

    @Override
    public boolean isWaitForExternalEvents() {
        return bprog.isWaitForExternalEvents();
    }

    @Override
    public boolean isMuteBreakPoints() {
        return debuggerEngine.isMuteBreakpoints();
    }

    private boolean checkStateEquals(RunnerState.State expectedState) {
        return expectedState.equals(state.getDebuggerState());
    }
    
    public String getDebuggerId() {
        return debuggerId;
    }
}
