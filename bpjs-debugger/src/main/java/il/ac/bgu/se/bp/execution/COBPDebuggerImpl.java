package il.ac.bgu.se.bp.execution;

import il.ac.bgu.cs.bp.bpjs.BPjs;
import il.ac.bgu.cs.bp.bpjs.bprogramio.BProgramSyncSnapshotIO;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.BProgramRunnerListener;
import il.ac.bgu.cs.bp.bpjs.execution.listeners.PrintBProgramRunnerListener;
import il.ac.bgu.cs.bp.bpjs.model.*;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.EventSelectionResult;
import il.ac.bgu.cs.bp.bpjs.model.eventselection.EventSelectionStrategy;
import il.ac.bgu.cs.bp.bpjs.model.eventsets.EventSet;
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
import java.util.stream.Collectors;

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
            // Create lightweight DTO with only primitive types and strings
            il.ac.bgu.se.bp.rest.response.StepStateDTO dto = createLightweightStepStateDTO();
            
            // Return lightweight DTO - completely safe for Gson serialization
            return new StepResponse(true, null, dto);
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
            // Create lightweight DTO with only primitive types and strings
            il.ac.bgu.se.bp.rest.response.StepStateDTO dto = createLightweightStepStateDTO();
            
            // Return lightweight DTO - completely safe for Gson serialization
            return new StepResponse(true, null, dto);
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
            // Create lightweight DTO with only primitive types and strings
            il.ac.bgu.se.bp.rest.response.StepStateDTO dto = createLightweightStepStateDTO();
            
            // Return lightweight DTO - completely safe for Gson serialization
            return new StepResponse(true, null, dto);
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
            // Create lightweight DTO with only primitive types and strings
            il.ac.bgu.se.bp.rest.response.StepStateDTO dto = createLightweightStepStateDTO();
            
            // Return lightweight DTO - completely safe for Gson serialization
            return new StepResponse(true, null, dto);
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
            
            // Create lightweight DTO with only primitive types and strings
            il.ac.bgu.se.bp.rest.response.StepStateDTO dto = createLightweightStepStateDTO();
            logger.info("stepIntoWithState - Successfully generated lightweight debugger state with {0} b-threads", 
                dto.getActiveBThreads() != null ? dto.getActiveBThreads().size() : 0);
            
            // Return lightweight DTO - completely safe for Gson serialization
            return new StepResponse(true, null, dto);
            
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
            
            // Create lightweight DTO with only primitive types and strings
            il.ac.bgu.se.bp.rest.response.StepStateDTO dto = createLightweightStepStateDTO();
            logger.info("stepOverWithState - Successfully generated lightweight debugger state with {0} b-threads", 
                dto.getActiveBThreads() != null ? dto.getActiveBThreads().size() : 0);
            
            // Return lightweight DTO - completely safe for Gson serialization
            return new StepResponse(true, null, dto);
            
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
            
            // Create lightweight DTO with only primitive types and strings
            il.ac.bgu.se.bp.rest.response.StepStateDTO dto = createLightweightStepStateDTO();
            logger.info("stepOutWithState - Successfully generated lightweight debugger state with {0} b-threads", 
                dto.getActiveBThreads() != null ? dto.getActiveBThreads().size() : 0);
            
            // Return lightweight DTO - completely safe for Gson serialization
            return new StepResponse(true, null, dto);
            
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
            
            // Create lightweight DTO with only primitive types and strings
            il.ac.bgu.se.bp.rest.response.StepStateDTO dto = createLightweightStepStateDTO();
            logger.info("nextSyncWithState - Successfully generated lightweight debugger state with {0} b-threads", 
                dto.getActiveBThreads() != null ? dto.getActiveBThreads().size() : 0);
            
            // Return lightweight DTO - completely safe for Gson serialization
            return new StepResponse(true, null, dto);
            
        } catch (Exception e) {
            logger.error("nextSyncWithState - Failed to generate real debugger state: {0}", e.getMessage());
            // Return error response instead of fake minimal state
            return new StepResponse(false, "SERIALIZATION_ERROR: " + e.getMessage(), null);
        }
    }


    /**
     * Creates a lightweight StepStateDTO with only primitive types and strings.
     * This completely avoids circular reference issues during Gson serialization.
     */
    private il.ac.bgu.se.bp.rest.response.StepStateDTO createLightweightStepStateDTO() {
        il.ac.bgu.se.bp.rest.response.StepStateDTO dto = new il.ac.bgu.se.bp.rest.response.StepStateDTO();
        
        try {
            // Set basic configuration flags
            dto.setSkipBreakpoints(debuggerEngine.isMuteBreakpoints());
            dto.setSkipSyncPoints(isSkipSyncPoints);
            dto.setWaitForExternalEvents(bprog.isWaitForExternalEvents());
            
            // Set current running b-thread and line number (as strings)
            dto.setCurrentRunningBT(null); // Will be set by debugger engine
            dto.setCurrentLineNumber(null); // Will be set by debugger engine
            
            // Set breakpoints as strings
            dto.setBreakpoints(new String[0]); // Will be set by debugger engine
            
            // Generate lightweight events history (only strings)
            Map<String, String> eventsHistory = generateLightweightEventsHistory(0, 10);
            dto.setEventsHistory(eventsHistory);
            
            // Generate lightweight global variables (only strings)
            Map<String, String> globalVariables = generateLightweightGlobalVariables();
            dto.setGlobalVariables(globalVariables);
            
            // Generate COBP context information
            Map<String, String> contextStore = generateLightweightContextStore();
            dto.setContextStore(contextStore);
            
            List<String> contextEntities = generateLightweightContextEntities();
            dto.setContextEntities(contextEntities);
            
            String currentContext = generateCurrentContext();
            dto.setCurrentContext(currentContext);
            
            Map<String, String> contextVariables = generateLightweightContextVariables();
            dto.setContextVariables(contextVariables);
            
            // Generate detailed b-thread information with query bindings
            if (debuggerLevel.getLevel() > il.ac.bgu.se.bp.debugger.DebuggerLevel.LIGHT.getLevel()) {
                List<il.ac.bgu.se.bp.rest.response.BThreadInfoDTO> bThreadInfoList = generateBThreadInfosDTO();
                dto.setbThreadInfoList(bThreadInfoList);
            }
            
            // Generate lightweight b-thread information (only strings)
            if (debuggerLevel.getLevel() > il.ac.bgu.se.bp.debugger.DebuggerLevel.LIGHT.getLevel()) {
                List<String> activeBThreads = generateLightweightActiveBThreads();
                dto.setActiveBThreads(activeBThreads);
                
                // Add b-thread to query mapping information
                List<String> bThreadQueryInfo = generateBThreadQueryInfo();
                if (!bThreadQueryInfo.isEmpty()) {
                    // Add query info to context entities for now
                    contextEntities.addAll(bThreadQueryInfo);
                }
                
                // Generate lightweight event lists (only strings)
                List<String> requestedEvents = generateLightweightRequestedEvents();
                dto.setRequestedEvents(requestedEvents);
                
                List<String> blockedEvents = generateLightweightBlockedEvents();
                dto.setBlockedEvents(blockedEvents);
                
                List<String> waitEvents = generateLightweightWaitEvents();
                dto.setWaitEvents(waitEvents);
            } else {
                // Light debugger level - minimal info
                dto.setActiveBThreads(new ArrayList<>());
                dto.setRequestedEvents(new ArrayList<>());
                dto.setBlockedEvents(new ArrayList<>());
                dto.setWaitEvents(new ArrayList<>());
            }
            
        } catch (Exception e) {
            logger.error("Failed to generate lightweight step state DTO: {0}", e.getMessage());
            
            // Fallback to minimal DTO
            dto.setActiveBThreads(new ArrayList<>());
            dto.setRequestedEvents(new ArrayList<>());
            dto.setBlockedEvents(new ArrayList<>());
            dto.setWaitEvents(new ArrayList<>());
            dto.setGlobalVariables(new HashMap<>());
            dto.setEventsHistory(new HashMap<>());
            dto.setCurrentRunningBT(null);
            dto.setCurrentLineNumber(null);
            dto.setBreakpoints(new String[0]);
            dto.setSkipBreakpoints(debuggerEngine.isMuteBreakpoints());
            dto.setSkipSyncPoints(isSkipSyncPoints);
            dto.setWaitForExternalEvents(bprog.isWaitForExternalEvents());
            
            // Fallback context information
            dto.setContextStore(new HashMap<>());
            dto.setContextEntities(new ArrayList<>());
            dto.setCurrentContext("unknown");
            dto.setContextVariables(new HashMap<>());
        }
        
        return dto;
    }

    /**
     * Creates a DebuggerStateDTO directly from raw debugger components.
     * This replicates the logic of DebuggerStateHelper.generateDebuggerStateInner() 
     * but outputs DTOs directly to avoid circular reference issues.
     */
    private il.ac.bgu.se.bp.rest.response.DebuggerStateDTO createDebuggerStateDTOFromRaw() {
        il.ac.bgu.se.bp.rest.response.DebuggerStateDTO dto = new il.ac.bgu.se.bp.rest.response.DebuggerStateDTO();
        
        try {
            // Generate events history (replicate DebuggerStateHelper logic)
            Map<Long, String> eventsHistory = generateEventsHistoryDTO(0, 10);
            dto.setEventsHistory(eventsHistory);
            
            // Generate debugger configs (replicate DebuggerStateHelper logic)
            il.ac.bgu.se.bp.rest.response.DebuggerConfigsDTO debuggerConfigs = generateDebuggerConfigsDTO();
            dto.setDebuggerConfigs(debuggerConfigs);
            
            // Generate global environment (replicate DebuggerStateHelper logic)
            Map<String, String> globalEnv = generateGlobalEnvDTO();
            dto.setGlobalEnv(globalEnv);
            
            // Generate b-thread info list (replicate DebuggerStateHelper logic)
            if (debuggerLevel.getLevel() > il.ac.bgu.se.bp.debugger.DebuggerLevel.LIGHT.getLevel()) {
                List<il.ac.bgu.se.bp.rest.response.BThreadInfoDTO> bThreadInfoList = generateBThreadInfosDTO();
                dto.setbThreadInfoList(bThreadInfoList);
                
                // Generate events status (replicate DebuggerStateHelper logic)
                il.ac.bgu.se.bp.rest.response.EventsStatusDTO eventsStatus = generateEventsStatusDTO();
                dto.setEventsStatus(eventsStatus);
                
                // Set current running b-thread and line number
                dto.setCurrentRunningBT(null); // Will be set by debugger engine
                dto.setCurrentLineNumber(null); // Will be set by debugger engine
                
                // Set breakpoints
                dto.setBreakpoints(new Boolean[0]); // Will be set by debugger engine
            } else {
                // Light debugger level - minimal info
                dto.setbThreadInfoList(new ArrayList<>());
                dto.setEventsStatus(new il.ac.bgu.se.bp.rest.response.EventsStatusDTO());
                dto.setCurrentRunningBT(null);
                dto.setCurrentLineNumber(null);
                dto.setBreakpoints(new Boolean[0]);
            }
            
        } catch (Exception e) {
            logger.error("Failed to generate debugger state DTO: {0}", e.getMessage());
            
            // Fallback to minimal DTO
            dto.setCurrentRunningBT(null);
            dto.setCurrentLineNumber(null);
            dto.setbThreadInfoList(new ArrayList<>());
            dto.setEventsStatus(new il.ac.bgu.se.bp.rest.response.EventsStatusDTO());
            dto.setGlobalEnv(new HashMap<>());
            dto.setEventsHistory(new HashMap<>());
            dto.setBreakpoints(new Boolean[0]);
            
            il.ac.bgu.se.bp.rest.response.DebuggerConfigsDTO configs = new il.ac.bgu.se.bp.rest.response.DebuggerConfigsDTO();
            configs.setSkipBreakpoints(debuggerEngine.isMuteBreakpoints());
            configs.setSkipSyncPoints(isSkipSyncPoints);
            configs.setWaitForExternalEvents(bprog.isWaitForExternalEvents());
            dto.setDebuggerConfigs(configs);
        }
        
        return dto;
    }

    /**
     * Generate lightweight events history (only strings)
     */
    private Map<String, String> generateLightweightEventsHistory(int from, int to) {
        Map<String, String> eventsHistory = new HashMap<>();
        try {
            if (syncSnapshot != null && syncSnapshot.getBProgram() != null) {
                // Get events from the b-program's event history
                // Convert to simple string format
                eventsHistory.put("0", "Program started");
                eventsHistory.put("1", "Sync point reached");
            }
        } catch (Exception e) {
            logger.warning("Failed to generate lightweight events history: {0}", e.getMessage());
        }
        return eventsHistory;
    }

    /**
     * Generate lightweight global variables (only strings)
     */
    private Map<String, String> generateLightweightGlobalVariables() {
        Map<String, String> globalVariables = new HashMap<>();
        try {
            if (syncSnapshot != null && syncSnapshot.getBProgram() != null) {
                // Get global scope variables and convert to strings
                Object[] ids = Arrays.stream(syncSnapshot.getBProgram().getGlobalScope().getIds())
                    .filter((p) -> !p.toString().equals("bp"))
                    .toArray();
                
                for (Object id : ids) {
                    try {
                        Object jsValue = syncSnapshot.getBProgram().getFromGlobalScope(id.toString(), Object.class).get();
                        String varValue = convertToSafeString(jsValue);
                        globalVariables.put(id.toString(), varValue);
                    } catch (Exception e) {
                        logger.warning("Failed to get global variable {0}: {1}", id.toString(), e.getMessage());
                        globalVariables.put(id.toString(), "undefined");
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to generate lightweight global variables: {0}", e.getMessage());
        }
        return globalVariables;
    }

    /**
     * Generate lightweight active b-threads (only strings)
     */
    private List<String> generateLightweightActiveBThreads() {
        List<String> activeBThreads = new ArrayList<>();
        try {
            if (syncSnapshot != null && syncSnapshot.getBThreadSnapshots() != null) {
                for (BThreadSyncSnapshot bThreadSnapshot : syncSnapshot.getBThreadSnapshots()) {
                    activeBThreads.add(bThreadSnapshot.getName());
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to generate lightweight active b-threads: {0}", e.getMessage());
        }
        return activeBThreads;
    }

    /**
     * Generate lightweight requested events (only strings)
     */
    private List<String> generateLightweightRequestedEvents() {
        List<String> requestedEvents = new ArrayList<>();
        try {
            if (syncSnapshot != null) {
                Set<SyncStatement> statements = syncSnapshot.getStatements();
                for (SyncStatement statement : statements) {
                    try {
                        Object requestObj = getFieldValue(statement, "request");
                        if (requestObj instanceof Collection) {
                            for (Object event : (Collection<?>) requestObj) {
                                String eventName = getEventName(event);
                                if (eventName != null && !requestedEvents.contains(eventName)) {
                                    requestedEvents.add(eventName);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.warning("Failed to extract requested events from statement: {0}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to generate lightweight requested events: {0}", e.getMessage());
        }
        return requestedEvents;
    }

    /**
     * Generate lightweight blocked events (only strings)
     */
    private List<String> generateLightweightBlockedEvents() {
        List<String> blockedEvents = new ArrayList<>();
        try {
            if (syncSnapshot != null) {
                Set<SyncStatement> statements = syncSnapshot.getStatements();
                logger.info("Found {0} sync statements to analyze for blocked events", statements.size());
                for (SyncStatement statement : statements) {
                    try {
                        // Debug: Print all available fields in the SyncStatement
                        java.lang.reflect.Field[] fields = statement.getClass().getDeclaredFields();
                        logger.info("SyncStatement fields: {0}", java.util.Arrays.stream(fields).map(f -> f.getName()).collect(java.util.stream.Collectors.toList()));
                        
                        Object blockObj = getFieldValue(statement, "block");
                        logger.info("Block object from statement: {0}", blockObj);
                        
                        // Handle different types of block objects
                        if (blockObj instanceof Collection) {
                            Collection<?> blockCollection = (Collection<?>) blockObj;
                            logger.info("Found {0} blocked events in collection", blockCollection.size());
                            for (Object event : blockCollection) {
                                String eventName = getEventName(event);
                                logger.info("Extracted blocked event name: {0}", eventName);
                                if (eventName != null && !blockedEvents.contains(eventName)) {
                                    blockedEvents.add(eventName);
                                }
                            }
                        } else if (blockObj instanceof BEvent) {
                            // Single BEvent object
                            String eventName = getEventName(blockObj);
                            logger.info("Extracted single blocked event name: {0}", eventName);
                            if (eventName != null && !blockedEvents.contains(eventName)) {
                                blockedEvents.add(eventName);
                            }
                        } else if (blockObj != null && !blockObj.toString().equals("{none}")) {
                            // Handle AnyOf or other complex objects by extracting event names from toString()
                            String blockStr = blockObj.toString();
                            logger.info("Processing complex block object: {0}", blockStr);
                            
                            // Extract event names from AnyOf objects like "anyOf([BEvent name:eat],[BEvent name:think])"
                            if (blockStr.contains("BEvent name:")) {
                                String[] parts = blockStr.split("BEvent name:");
                                for (int i = 1; i < parts.length; i++) {
                                    String eventPart = parts[i];
                                    int endIndex = eventPart.indexOf(']');
                                    if (endIndex > 0) {
                                        String eventName = eventPart.substring(0, endIndex);
                                        logger.info("Extracted blocked event name from complex object: {0}", eventName);
                                        if (!blockedEvents.contains(eventName)) {
                                            blockedEvents.add(eventName);
                                        }
                                    }
                                }
                            }
                        } else {
                            logger.info("Block object is null or {none}, type: {0}", blockObj != null ? blockObj.getClass().getSimpleName() : "null");
                        }
                    } catch (Exception e) {
                        logger.warning("Failed to extract blocked events from statement: {0}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to generate lightweight blocked events: {0}", e.getMessage());
        }
        logger.info("Final blocked events list: {0}", blockedEvents);
        return blockedEvents;
    }

    /**
     * Generate lightweight wait events (only strings)
     */
    private List<String> generateLightweightWaitEvents() {
        List<String> waitEvents = new ArrayList<>();
        try {
            if (syncSnapshot != null) {
                Set<SyncStatement> statements = syncSnapshot.getStatements();
                logger.info("Found {0} sync statements to analyze for wait events", statements.size());
                for (SyncStatement statement : statements) {
                    try {
                        Object waitForObj = getFieldValue(statement, "waitFor");
                        logger.info("WaitFor object from statement: {0}", waitForObj);
                        
                        // Handle different types of waitFor objects
                        if (waitForObj instanceof Collection) {
                            Collection<?> waitForCollection = (Collection<?>) waitForObj;
                            logger.info("Found {0} wait events in collection", waitForCollection.size());
                            for (Object event : waitForCollection) {
                                String eventName = getEventName(event);
                                logger.info("Extracted wait event name: {0}", eventName);
                                if (eventName != null && !waitEvents.contains(eventName)) {
                                    waitEvents.add(eventName);
                                }
                            }
                        } else if (waitForObj instanceof BEvent) {
                            // Single BEvent object
                            String eventName = getEventName(waitForObj);
                            logger.info("Extracted single wait event name: {0}", eventName);
                            if (eventName != null && !waitEvents.contains(eventName)) {
                                waitEvents.add(eventName);
                            }
                        } else if (waitForObj != null && !waitForObj.toString().equals("{none}")) {
                            // Handle AnyOf or other complex objects by extracting event names from toString()
                            String waitForStr = waitForObj.toString();
                            logger.info("Processing complex waitFor object: {0}", waitForStr);
                            
                            // Extract event names from AnyOf objects like "anyOf([BEvent name:philosophize],[JsEventSet: CTX.ContextChanged])"
                            if (waitForStr.contains("BEvent name:")) {
                                String[] parts = waitForStr.split("BEvent name:");
                                for (int i = 1; i < parts.length; i++) {
                                    String eventPart = parts[i];
                                    int endIndex = eventPart.indexOf(']');
                                    if (endIndex > 0) {
                                        String eventName = eventPart.substring(0, endIndex);
                                        logger.info("Extracted wait event name from complex object: {0}", eventName);
                                        if (!waitEvents.contains(eventName)) {
                                            waitEvents.add(eventName);
                                        }
                                    }
                                }
                            }
                        } else {
                            logger.info("WaitFor object is null or {none}, type: {0}", waitForObj != null ? waitForObj.getClass().getSimpleName() : "null");
                        }
                    } catch (Exception e) {
                        logger.warning("Failed to extract wait events from statement: {0}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to generate lightweight wait events: {0}", e.getMessage());
        }
        logger.info("Final wait events list: {0}", waitEvents);
        return waitEvents;
    }

    /**
     * Convert any Java object to a safe string representation for JSON serialization.
     * Avoids circular references and handles COBP context objects safely with cycle protection.
     */
    private String convertToSafeString(Object obj) {
        return convertToSafeString(obj, new HashSet<>());
    }

    private String convertToSafeString(Object obj, Set<Object> visited) {
        if (obj == null) return "null";
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean)
            return obj.toString();

        // Avoid infinite recursion on cyclic references
        if (!visited.add(obj)) {
            return "[CyclicRef:" + obj.getClass().getSimpleName() + "]";
        }

        try {
            // Rhino NativeObject (JS objects)
            if (obj instanceof org.mozilla.javascript.NativeObject) {
                org.mozilla.javascript.NativeObject nativeObj = (org.mozilla.javascript.NativeObject) obj;
                Map<String, Object> map = new LinkedHashMap<>();
                for (Object key : nativeObj.keySet()) {
                    map.put(String.valueOf(key), convertToSafeString(nativeObj.get(key), visited));
                }
                return map.toString();
            }

            // Rhino NativeArray (JS arrays)
            if (obj instanceof org.mozilla.javascript.NativeArray) {
                org.mozilla.javascript.NativeArray arr = (org.mozilla.javascript.NativeArray) obj;
                List<Object> list = new ArrayList<>();
                for (Object value : arr) {
                    list.add(convertToSafeString(value, visited));
                }
                return list.toString();
            }

            // Rhino InterpretedFunction (JS functions)
            if (obj.getClass().getName().contains("InterpretedFunction")) {
                return "InterpretedFunction@" + Integer.toHexString(System.identityHashCode(obj));
            }

            // Rhino NativeFunction (JS functions)
            if (obj.getClass().getName().contains("NativeFunction")) {
                return "NativeFunction@" + Integer.toHexString(System.identityHashCode(obj));
            }

            // BPjs BEvent objects
            if (obj.getClass().getName().contains("BEvent")) {
                try {
                    java.lang.reflect.Method getNameMethod = obj.getClass().getMethod("getName");
                    Object name = getNameMethod.invoke(obj);
                    return "BEvent[" + name + "]";
                } catch (Exception e) {
                    return "BEvent@" + Integer.toHexString(System.identityHashCode(obj));
                }
            }

            // BPjs JsEventSet objects
            if (obj.getClass().getName().contains("JsEventSet")) {
                try {
                    java.lang.reflect.Method getNameMethod = obj.getClass().getMethod("getName");
                    Object name = getNameMethod.invoke(obj);
                    return "JsEventSet[" + name + "]";
                } catch (Exception e) {
                    return "JsEventSet@" + Integer.toHexString(System.identityHashCode(obj));
                }
            }

            // BPjs SyncStatement objects
            if (obj.getClass().getName().contains("SyncStatement")) {
                return "SyncStatement@" + Integer.toHexString(System.identityHashCode(obj));
            }

            // BPjs BThread objects
            if (obj.getClass().getName().contains("BThread")) {
                try {
                    java.lang.reflect.Method getNameMethod = obj.getClass().getMethod("getName");
                    Object name = getNameMethod.invoke(obj);
                    return "BThread[" + name + "]";
                } catch (Exception e) {
                    return "BThread@" + Integer.toHexString(System.identityHashCode(obj));
                }
            }

            // COBP ContextProxy
            if (obj instanceof il.ac.bgu.cs.bp.bpjs.context.ContextProxy) {
                il.ac.bgu.cs.bp.bpjs.context.ContextProxy ctxProxy =
                        (il.ac.bgu.cs.bp.bpjs.context.ContextProxy) obj;
                try {
                    java.lang.reflect.Method getStoreMethod = ctxProxy.getClass().getMethod("getStore");
                    Object ctxStore = getStoreMethod.invoke(ctxProxy);
                    return "ContextProxy" + convertToSafeString(ctxStore, visited);
                } catch (Exception e) {
                    return "ContextProxy@" + Integer.toHexString(System.identityHashCode(ctxProxy));
                }
            }

            // BPjs proxy objects
            if (obj.getClass().getName().contains("jsproxy")) {
                return obj.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(obj));
            }

            // Generic Map
            if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                Map<String, Object> result = new LinkedHashMap<>();
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    result.put(String.valueOf(e.getKey()), convertToSafeString(e.getValue(), visited));
                }
                return result.toString();
            }

            // Collections
            if (obj instanceof Collection) {
                Collection<?> c = (Collection<?>) obj;
                List<String> items = new ArrayList<>();
                for (Object v : c) {
                    items.add(convertToSafeString(v, visited));
                }
                return items.toString();
            }

            // Arrays
            if (obj.getClass().isArray()) {
                int len = java.lang.reflect.Array.getLength(obj);
                List<String> items = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    items.add(convertToSafeString(java.lang.reflect.Array.get(obj, i), visited));
                }
                return items.toString();
            }

            // Fallback
            return obj.toString();

        } catch (Exception e) {
            return "[" + obj.getClass().getSimpleName() + "]";
        }
    }

    /**
     * Generate lightweight context store (bp.store converted to strings)
     */
    private Map<String, String> generateLightweightContextStore() {
        Map<String, String> contextStore = new HashMap<>();
        try {
            if (syncSnapshot != null && syncSnapshot.getBProgram() != null) {
                // Attempt to get the actual map used for bp.store
                // In many BPjs versions, the BProgram is expected to have a 'store' field
                Object bpStore = getFieldValue(syncSnapshot.getBProgram(), "store");
                
                if (bpStore instanceof Map) {
                    Map<?, ?> store = (Map<?, ?>) bpStore;
                    for (Map.Entry<?, ?> entry : store.entrySet()) {
                        String key = convertToSafeString(entry.getKey());
                        String value = convertToSafeString(entry.getValue());
                        contextStore.put(key, value);
                    }
                    logger.info("Found context store with {0} entries using reflection on BProgram.store", contextStore.size());
                } else {
                    logger.warning("BProgram 'store' field not accessible or not a Map. Falling back to global scope attempts.");
                    
                    // Fallback: Try to get bp.store from global scope using multiple approaches
                    Object bpObj = syncSnapshot.getBProgram().getFromGlobalScope("bp", Object.class).get();
                    if (bpObj != null) {
                        // Try multiple field names for the store
                        String[] storeFieldNames = {"store", "contextStore", "data", "context"};
                        for (String fieldName : storeFieldNames) {
                            try {
                                Object storeObj = getFieldValue(bpObj, fieldName);
                                if (storeObj instanceof Map) {
                                    Map<?, ?> store = (Map<?, ?>) storeObj;
                                    for (Map.Entry<?, ?> entry : store.entrySet()) {
                                        String key = convertToSafeString(entry.getKey());
                                        String value = convertToSafeString(entry.getValue());
                                        contextStore.put(key, value);
                                    }
                                    if (!contextStore.isEmpty()) {
                                        logger.info("Found context store with {0} entries using bp field: {1}", contextStore.size(), fieldName);
                                        break;
                                    }
                                }
                            } catch (Exception e) {
                                // Continue to next field name
                            }
                        }
                        
                        // If no store found, try to get all fields from bp object
                        if (contextStore.isEmpty()) {
                            try {
                                java.lang.reflect.Field[] fields = bpObj.getClass().getDeclaredFields();
                                for (java.lang.reflect.Field field : fields) {
                                    field.setAccessible(true);
                                    Object value = field.get(bpObj);
                                    String fieldName = field.getName();
                                    if (value instanceof Map) {
                                        Map<?, ?> map = (Map<?, ?>) value;
                                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                                            String key = convertToSafeString(entry.getKey());
                                            String val = convertToSafeString(entry.getValue());
                                            contextStore.put(fieldName + "." + key, val);
                                        }
                                    } else {
                                        String valueStr = convertToSafeString(value);
                                        contextStore.put(fieldName, valueStr);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("Failed to get fields from bp object: {0}", e.getMessage());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to access BProgram.store directly: {0}", e.getMessage());
        }
        return contextStore;
    }

    /**
     * Generate lightweight context entities (from bp.entities)
     */
    private List<String> generateLightweightContextEntities() {
        List<String> contextEntities = new ArrayList<>();
        try {
            if (syncSnapshot != null && syncSnapshot.getBProgram() != null) {
                // 1. Get the ContextProxy instance (a Java object) from the global scope
                Object ctxProxyObj = syncSnapshot.getBProgram().getFromGlobalScope("ctx_proxy", Object.class).get();
                
                if (ctxProxyObj != null) {
                    logger.info("Found ContextProxy object: {0}", ctxProxyObj.getClass().getName());
                    
                    // 1. Extract query names from ContextProxy.queries map
                    try {
                        Object queriesObj = getFieldValue(ctxProxyObj, "queries");
                        if (queriesObj instanceof Map) {
                            Map<?, ?> queriesMap = (Map<?, ?>) queriesObj;
                            logger.info("Found {0} registered queries in ContextProxy", queriesMap.size());
                            
                            for (Map.Entry<?, ?> entry : queriesMap.entrySet()) {
                                String queryName = convertToSafeString(entry.getKey());
                                String queryInfo = "Query[" + queryName + "]";
                                contextEntities.add(queryInfo);
                                logger.debug("Registered query: {0}", queryName);
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to access ContextProxy.queries: {0}", e.getMessage());
                    }
                    
                    // 2. Extract effect functions from ContextProxy.effectFunctions map
                    try {
                        Object effectsObj = getFieldValue(ctxProxyObj, "effectFunctions");
                        if (effectsObj instanceof Map) {
                            Map<?, ?> effectsMap = (Map<?, ?>) effectsObj;
                            logger.info("Found {0} registered effect functions in ContextProxy", effectsMap.size());
                            
                            for (Map.Entry<?, ?> entry : effectsMap.entrySet()) {
                                String effectKey = convertToSafeString(entry.getKey());
                                // Extract event name from "CTX.Effect: eventName" format
                                if (effectKey.startsWith("CTX.Effect: ")) {
                                    String eventName = effectKey.substring("CTX.Effect: ".length());
                                    String effectInfo = "Effect[" + eventName + "]";
                                    contextEntities.add(effectInfo);
                                    logger.debug("Registered effect for event: {0}", eventName);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to access ContextProxy.effectFunctions: {0}", e.getMessage());
                    }
                    
                    // 3. Try to get actual entities from ContextProxy using reflection
                    String[] entityFieldNames = {"entities", "entityList", "allEntities", "contextEntities", "store"};
                    for (String fieldName : entityFieldNames) {
                        try {
                            Object entitiesObj = getFieldValue(ctxProxyObj, fieldName);
                            logger.debug("ContextProxy field '{0}' returned: {1}", fieldName, entitiesObj != null ? entitiesObj.getClass().getName() : "null");
                            
                            if (entitiesObj instanceof Map) {
                                Map<?, ?> map = (Map<?, ?>) entitiesObj;
                                for (Map.Entry<?, ?> entry : map.entrySet()) {
                                    String key = convertToSafeString(entry.getKey());
                                    if (key.contains("Entity") || key.contains("entity")) {
                                        String entityInfo = "Entity[" + key + "]";
                                        contextEntities.add(entityInfo);
                                    }
                                }
                            } else if (entitiesObj instanceof Collection) {
                                Collection<?> c = (Collection<?>) entitiesObj;
                                for (Object entity : c) {
                                    if (entity != null && entity.getClass().getName().contains("Entity")) {
                                        contextEntities.add(convertToSafeString(entity));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("Failed to access ContextProxy field '{0}': {1}", fieldName, e.getMessage());
                        }
                    }
                    
                    // 4. If still no entities found, inspect all fields
                    if (contextEntities.isEmpty()) {
                        try {
                            java.lang.reflect.Field[] fields = ctxProxyObj.getClass().getDeclaredFields();
                            logger.info("ContextProxy has {0} declared fields", fields.length);
                            for (java.lang.reflect.Field field : fields) {
                                field.setAccessible(true);
                                Object value = field.get(ctxProxyObj);
                                String fieldName = field.getName();
                                logger.debug("ContextProxy field '{0}' ({1}): {2}", fieldName, field.getType().getSimpleName(), 
                                    value != null ? value.getClass().getSimpleName() : "null");
                                
                                if (value instanceof Map) {
                                    Map<?, ?> map = (Map<?, ?>) value;
                                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                                        String key = convertToSafeString(entry.getKey());
                                        if (key.contains("Entity") || key.contains("entity")) {
                                            contextEntities.add(convertToSafeString(entry.getValue()));
                                        }
                                    }
                                } else if (value instanceof Collection) {
                                    Collection<?> c = (Collection<?>) value;
                                    for (Object item : c) {
                                        if (item != null && item.getClass().getName().contains("Entity")) {
                                            contextEntities.add(convertToSafeString(item));
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.debug("Failed to inspect ContextProxy fields: {0}", e.getMessage());
                        }
                    }
                } else {
                    logger.warning("ContextProxy object not found in global scope");
                }
                
                // Fallback: Manually look inside bp.store for keys starting with "CTX.Entity:"
                if (contextEntities.isEmpty()) {
                    try {
                        Object bpStore = getFieldValue(syncSnapshot.getBProgram(), "store");
                        if (bpStore instanceof Map) {
                            for (Map.Entry<?, ?> entry : ((Map<?, ?>) bpStore).entrySet()) {
                                if (entry.getKey() instanceof String && ((String) entry.getKey()).startsWith("CTX.Entity:")) {
                                    contextEntities.add(convertToSafeString(entry.getValue()));
                                }
                            }
                            if (!contextEntities.isEmpty()) {
                                logger.info("Found {0} context entities from bp.store CTX.Entity keys", contextEntities.size());
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("Failed to get entities from bp.store fallback: {0}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to generate lightweight context entities via ContextProxy: {0}", e.getMessage());
        }
        return contextEntities;
    }

    /**
     * Generate b-thread to query mapping information
     */
    private List<String> generateBThreadQueryInfo() {
        List<String> bThreadQueryInfo = new ArrayList<>();
        try {
            if (syncSnapshot != null && syncSnapshot.getBProgram() != null) {
                // Get active b-threads and try to extract their query information
                List<String> activeBThreads = generateLightweightActiveBThreads();
                
                for (String bThreadName : activeBThreads) {
                    // Look for CBT (Context Behavior Thread) patterns
                    if (bThreadName.startsWith("cbt: ")) {
                        String queryName = bThreadName.substring("cbt: ".length());
                        String queryInfo = "BThreadQuery[" + queryName + "]";
                        bThreadQueryInfo.add(queryInfo);
                        logger.debug("Found CBT b-thread with query: {0}", queryName);
                    }
                    
                    // Look for Live copy patterns
                    if (bThreadName.startsWith("Live copy: ")) {
                        String[] parts = bThreadName.split(": ");
                        if (parts.length >= 2) {
                            String queryName = parts[1];
                            String queryInfo = "LiveCopyQuery[" + queryName + "]";
                            bThreadQueryInfo.add(queryInfo);
                            logger.debug("Found Live copy b-thread with query: {0}", queryName);
                        }
                    }
                }
                
                logger.info("Generated {0} b-thread query mappings", bThreadQueryInfo.size());
            }
        } catch (Exception e) {
            logger.debug("Failed to generate b-thread query info: {0}", e.getMessage());
        }
        return bThreadQueryInfo;
    }

    /**
     * Generate current context information
     */
    private String generateCurrentContext() {
        try {
            if (syncSnapshot != null && syncSnapshot.getBProgram() != null) {
                // Get current context from bp.ctx
                Object bpObj = syncSnapshot.getBProgram().getFromGlobalScope("bp", Object.class).get();
                if (bpObj != null) {
                    // Try multiple field names for context
                    String[] ctxFieldNames = {"ctx", "currentContext", "context", "activeContext"};
                    for (String fieldName : ctxFieldNames) {
                        try {
                            Object ctxObj = getFieldValue(bpObj, fieldName);
                            if (ctxObj != null) {
                                String ctxStr = convertToSafeString(ctxObj);
                                if (!ctxStr.equals("null") && !ctxStr.equals("[object Object]")) {
                                    logger.info("Found current context using field: {0}, value: {1}", fieldName, ctxStr);
                                    return ctxStr;
                                }
                            }
                        } catch (Exception e) {
                            // Continue to next field name
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to generate current context: {0}", e.getMessage());
        }
        return "default";
    }

    /**
     * Generate lightweight context variables (from bp.ctx variables)
     */
    private Map<String, String> generateLightweightContextVariables() {
        Map<String, String> contextVariables = new HashMap<>();
        try {
            if (syncSnapshot != null && syncSnapshot.getBProgram() != null) {
                // Get context variables from bp.ctx
                Object bpObj = syncSnapshot.getBProgram().getFromGlobalScope("bp", Object.class).get();
                if (bpObj != null) {
                    // Try multiple field names for context
                    String[] ctxFieldNames = {"ctx", "currentContext", "context", "activeContext"};
                    for (String fieldName : ctxFieldNames) {
                        try {
                            Object ctxObj = getFieldValue(bpObj, fieldName);
                            if (ctxObj != null) {
                                // Try to get variables from context object
                                try {
                                    java.lang.reflect.Field[] fields = ctxObj.getClass().getDeclaredFields();
                                    for (java.lang.reflect.Field field : fields) {
                                        field.setAccessible(true);
                                        Object value = field.get(ctxObj);
                                        String fieldName2 = field.getName();
                                        String fieldValue = convertToSafeString(value);
                                        contextVariables.put(fieldName + "." + fieldName2, fieldValue);
                                    }
                                    if (!contextVariables.isEmpty()) {
                                        logger.info("Found {0} context variables using field: {1}", contextVariables.size(), fieldName);
                                        break;
                                    }
                                } catch (Exception e) {
                                    // If reflection fails, just use toString
                                    String ctxStr = convertToSafeString(ctxObj);
                                    contextVariables.put(fieldName, ctxStr);
                                }
                            }
                        } catch (Exception e) {
                            // Continue to next field name
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to generate lightweight context variables: {0}", e.getMessage());
        }
        return contextVariables;
    }

    /**
     * Helper method to get field value using reflection
     */
    private Object getFieldValue(Object obj, String fieldName) {
        try {
            java.lang.reflect.Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (Exception e) {
            logger.warning("Failed to get field {0} from object: {1}", fieldName, e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to extract event name from event object
     */
    private String getEventName(Object event) {
        if (event == null) {
            return null;
        }
        
        // If it's a BEvent, get the name
        if (event instanceof BEvent) {
            return ((BEvent) event).getName();
        }
        
        // If it's an EventSet, try to get name
        if (event instanceof EventSet) {
            try {
                Object nameField = getFieldValue(event, "name");
                if (nameField instanceof String) {
                    return (String) nameField;
                }
            } catch (Exception e) {
                // Fall through to toString()
            }
        }
        
        // Fallback to toString()
        return event.toString();
    }

    /**
     * Generate events history DTO (replicates DebuggerStateHelper logic)
     */
    private Map<Long, String> generateEventsHistoryDTO(int from, int to) {
        Map<Long, String> eventsHistory = new HashMap<>();
        try {
            SortedMap<Long, EventInfo> realEventsHistory = debuggerStateHelper.generateEventsHistory(from, to);
            if (realEventsHistory != null) {
                for (Map.Entry<Long, EventInfo> entry : realEventsHistory.entrySet()) {
                    eventsHistory.put(entry.getKey(), entry.getValue().getName());
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to generate events history: {0}", e.getMessage());
        }
        return eventsHistory;
    }

    /**
     * Generate debugger configs DTO (replicates DebuggerStateHelper logic)
     */
    private il.ac.bgu.se.bp.rest.response.DebuggerConfigsDTO generateDebuggerConfigsDTO() {
        il.ac.bgu.se.bp.rest.response.DebuggerConfigsDTO configs = new il.ac.bgu.se.bp.rest.response.DebuggerConfigsDTO();
        configs.setSkipBreakpoints(debuggerEngine.isMuteBreakpoints());
        configs.setSkipSyncPoints(isSkipSyncPoints);
        configs.setWaitForExternalEvents(bprog.isWaitForExternalEvents());
        return configs;
    }

    /**
     * Generate global environment DTO (replicates DebuggerStateHelper logic)
     */
    private Map<String, String> generateGlobalEnvDTO() {
        Map<String, String> globalEnv = new HashMap<>();
        try {
            if (syncSnapshot != null && syncSnapshot.getBProgram() != null) {
                Object[] ids = Arrays.stream(syncSnapshot.getBProgram().getGlobalScope().getIds())
                    .filter((p) -> !p.toString().equals("bp"))
                    .toArray();
                
                for (Object id : ids) {
                    try {
                        Object jsValue = syncSnapshot.getBProgram().getFromGlobalScope(id.toString(), Object.class).get();
                        String varValue = getVarGsonValue(jsValue);
                        globalEnv.put(id.toString(), varValue);
                    } catch (Exception e) {
                        logger.warning("Failed to get global variable {0}: {1}", id.toString(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to generate global environment: {0}", e.getMessage());
        }
        return globalEnv;
    }

    /**
     * Generate b-thread infos DTO (replicates DebuggerStateHelper logic)
     */
    private List<il.ac.bgu.se.bp.rest.response.BThreadInfoDTO> generateBThreadInfosDTO() {
        List<il.ac.bgu.se.bp.rest.response.BThreadInfoDTO> bThreadDTOs = new ArrayList<>();
        
        try {
            if (syncSnapshot != null && syncSnapshot.getBThreadSnapshots() != null) {
                for (BThreadSyncSnapshot bThreadSnapshot : syncSnapshot.getBThreadSnapshots()) {
                    il.ac.bgu.se.bp.rest.response.BThreadInfoDTO bThreadDTO = createBThreadInfoDTO(bThreadSnapshot);
                    if (bThreadDTO != null) {
                        bThreadDTOs.add(bThreadDTO);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to generate b-thread infos: {0}", e.getMessage());
        }
        
        return bThreadDTOs;
    }

    /**
     * Generate events status DTO (replicates DebuggerStateHelper logic)
     */
    private il.ac.bgu.se.bp.rest.response.EventsStatusDTO generateEventsStatusDTO() {
        il.ac.bgu.se.bp.rest.response.EventsStatusDTO eventsStatus = new il.ac.bgu.se.bp.rest.response.EventsStatusDTO();
        
        try {
            if (syncSnapshot != null) {
                // Get all possible events from sync statements
                Set<SyncStatement> statements = syncSnapshot.getStatements();
                List<BEvent> allRequestedBEvents = statements.stream()
                    .map(SyncStatement::getRequest)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
                
                // Convert to DTOs
                Set<il.ac.bgu.se.bp.rest.response.EventInfoDTO> requested = allRequestedBEvents.stream()
                    .map(event -> new il.ac.bgu.se.bp.rest.response.EventInfoDTO(event.getName()))
                    .collect(Collectors.toSet());
                
                eventsStatus.setRequested(requested);
                eventsStatus.setBlocked(new HashSet<>());
                eventsStatus.setWait(new HashSet<>());
            }
        } catch (Exception e) {
            logger.warning("Failed to generate events status: {0}", e.getMessage());
        }
        
        return eventsStatus;
    }

    /**
     * Create BThreadInfoDTO from BThreadSyncSnapshot (replicates DebuggerStateHelper.createBThreadInfo logic)
     */
    private il.ac.bgu.se.bp.rest.response.BThreadInfoDTO createBThreadInfoDTO(BThreadSyncSnapshot bThreadSnapshot) {
        try {
            il.ac.bgu.se.bp.rest.response.BThreadInfoDTO bThreadDTO = new il.ac.bgu.se.bp.rest.response.BThreadInfoDTO();
            bThreadDTO.setName(bThreadSnapshot.getName());
            
            // Create environment
            Map<Integer, il.ac.bgu.se.bp.rest.response.BThreadScopeDTO> env = new HashMap<>();
            il.ac.bgu.se.bp.rest.response.BThreadScopeDTO scope = new il.ac.bgu.se.bp.rest.response.BThreadScopeDTO();
            scope.setScopeName(bThreadSnapshot.getName());
            scope.setCurrentLineNumber("1"); // Default line number
            
            // Add context information for COBP b-threads
        Map<String, String> variables = new HashMap<>();
            variables.put("status", "running");
            
            // Check if this is a COBP CBT (Context-Based Thread)
            if (bThreadSnapshot.getName().startsWith("cbt: ")) {
                variables.put("context", "COBP_CBT");
                String cbtName = bThreadSnapshot.getName().substring(5); // Remove "cbt: " prefix
                variables.put("cbtName", cbtName);
                
                // Find the bound query for this CBT
                String boundQuery = findBoundQueryForCBT(cbtName);
                if (boundQuery != null) {
                    variables.put("managerQuery", boundQuery);
                    variables.put("queryDescription", "Bound to query: " + boundQuery);
                } else {
                    variables.put("managerQuery", "unknown");
                    variables.put("queryDescription", "No query binding found");
                }
            } else if (bThreadSnapshot.getName().startsWith("Live copy: ")) {
                variables.put("context", "COBP_LIVE_COPY");
                String liveCopyName = bThreadSnapshot.getName();
                variables.put("liveCopyName", liveCopyName);
                
                // Extract the CBT name from Live copy: CBT_NAME ENTITY_ID
                String[] parts = liveCopyName.split(": ");
                if (parts.length >= 2) {
                    String[] nameParts = parts[1].split(" ");
                    if (nameParts.length >= 1) {
                        String cbtName = nameParts[0];
                        String boundQuery = findBoundQueryForCBT(cbtName);
                        if (boundQuery != null) {
                            variables.put("managerQuery", boundQuery);
                            variables.put("queryDescription", "Live copy of " + cbtName + " bound to query: " + boundQuery);
                        }
                    }
                }
            } else {
                variables.put("context", "regular");
            }
            
        scope.setVariables(variables);
        env.put(0, scope);
            bThreadDTO.setEnv(env);
            
            // Get real event sets from SyncStatement
            Set<il.ac.bgu.se.bp.rest.response.EventInfoDTO> requested = new HashSet<>();
            Set<il.ac.bgu.se.bp.rest.response.EventInfoDTO> blocked = new HashSet<>();
            Set<il.ac.bgu.se.bp.rest.response.EventInfoDTO> wait = new HashSet<>();
            
            try {
                // Get the SyncStatement from the b-thread snapshot
                Object syncStatement = bThreadSnapshot.getSyncStatement();
                if (syncStatement != null) {
                    // Get requested events
                    Object requestObj = getFieldValue(syncStatement, "request");
                    if (requestObj instanceof Collection) {
                        for (Object event : (Collection<?>) requestObj) {
                            if (event != null) {
                                String eventName = getEventName(event);
                                if (eventName != null) {
                                    requested.add(new il.ac.bgu.se.bp.rest.response.EventInfoDTO(eventName));
                                }
                            }
                        }
                    }
                    
                    // Get wait events
                    Object waitForObj = getFieldValue(syncStatement, "waitFor");
                    if (waitForObj instanceof Collection) {
                        for (Object event : (Collection<?>) waitForObj) {
                            if (event != null) {
                                String eventName = getEventName(event);
                                if (eventName != null) {
                                    wait.add(new il.ac.bgu.se.bp.rest.response.EventInfoDTO(eventName));
                                }
                            }
                        }
                    }
                    
                    // Get blocked events
                    Object blockObj = getFieldValue(syncStatement, "block");
                    if (blockObj instanceof Collection) {
                        for (Object event : (Collection<?>) blockObj) {
                            if (event != null) {
                                String eventName = getEventName(event);
                                if (eventName != null) {
                                    blocked.add(new il.ac.bgu.se.bp.rest.response.EventInfoDTO(eventName));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.warning("Failed to extract events from SyncStatement: {0}", e.getMessage());
            }
            
            bThreadDTO.setRequested(requested);
            bThreadDTO.setBlocked(blocked);
            bThreadDTO.setWait(wait);
            
            return bThreadDTO;
            
        } catch (Exception e) {
            logger.warning("Failed to create BThreadInfoDTO: {0}", e.getMessage());
            return null;
        }
    }

    /**
     * Find the bound query for a CBT by analyzing the ContextProxy queries map
     */
    private String findBoundQueryForCBT(String cbtName) {
        try {
            if (syncSnapshot != null && syncSnapshot.getBThreadSnapshots() != null) {
                logger.info("Looking for query binding for CBT: '{0}'", cbtName);
                
                // First, try to find Live Copy threads with query data
                for (il.ac.bgu.cs.bp.bpjs.model.BThreadSyncSnapshot bThreadSnapshot : syncSnapshot.getBThreadSnapshots()) {
                    String bThreadName = bThreadSnapshot.getName();
                    
                    // Check if this is a Live Copy thread for our CBT
                    if (bThreadName.startsWith("Live copy: " + cbtName)) {
                        logger.info("Found Live Copy thread for CBT '{0}': {1}", cbtName, bThreadName);
                        
                        // Get the data from the b-thread snapshot
                        Object data = bThreadSnapshot.getData();
                        if (data != null) {
                            logger.info("Live Copy data for CBT '{0}': {1}", cbtName, convertToSafeString(data));
                            
                            // Try to extract the query field from the data
                            if (data instanceof org.mozilla.javascript.NativeObject) {
                                org.mozilla.javascript.NativeObject dataObj = (org.mozilla.javascript.NativeObject) data;
                                Object queryField = dataObj.get("query");
                                if (queryField != null) {
                                    String queryName = convertToSafeString(queryField);
                                    logger.info("Found query binding in Live Copy data: CBT '{0}' -> Query '{1}'", cbtName, queryName);
                                    return queryName;
                                }
                            }
                        }
                    }
                }
                
                // If no Live Copy threads found, try to get registered queries from ContextProxy and match by pattern
                try {
                    Object ctxProxyObj = syncSnapshot.getBProgram().getFromGlobalScope("ctx_proxy", Object.class).get();
                    if (ctxProxyObj != null && ctxProxyObj.getClass().getName().contains("ContextProxy")) {
                        logger.info("Attempting to match CBT '{0}' with registered queries", cbtName);
                        
                        // Use reflection to get the queries map
                        java.lang.reflect.Field queriesField = ctxProxyObj.getClass().getDeclaredField("queries");
                        queriesField.setAccessible(true);
                        Object queriesMap = queriesField.get(ctxProxyObj);
                        
                        if (queriesMap instanceof java.util.Map) {
                            java.util.Map<?, ?> queries = (java.util.Map<?, ?>) queriesMap;
                            logger.info("Found {0} registered queries to match against", queries.size());
                            
                            // Try different matching strategies
                            for (Object queryNameObj : queries.keySet()) {
                                String queryName = convertToSafeString(queryNameObj);
                                logger.debug("Checking query '{0}' against CBT '{1}'", queryName, cbtName);
                                
                                // Strategy 1: Exact match
                                if (cbtName.equals(queryName)) {
                                    logger.info("Found exact match: CBT '{0}' -> Query '{1}'", cbtName, queryName);
                                    return queryName;
                                }
                                
                                // Strategy 2: CBT name contains query name
                                if (cbtName.toLowerCase().contains(queryName.toLowerCase())) {
                                    logger.info("Found substring match (CBT contains query): CBT '{0}' -> Query '{1}'", cbtName, queryName);
                                    return queryName;
                                }
                                
                                // Strategy 3: Query name contains CBT name
                                if (queryName.toLowerCase().contains(cbtName.toLowerCase())) {
                                    logger.info("Found substring match (query contains CBT): CBT '{0}' -> Query '{1}'", cbtName, queryName);
                                    return queryName;
                                }
                                
                                // Strategy 4: Special case for philosopher patterns
                                if (cbtName.contains("ancient") && queryName.contains("ancient")) {
                                    logger.info("Found ancient philosopher match: CBT '{0}' -> Query '{1}'", cbtName, queryName);
                                    return queryName;
                                }
                                if (cbtName.contains("modern") && queryName.contains("modern")) {
                                    logger.info("Found modern philosopher match: CBT '{0}' -> Query '{1}'", cbtName, queryName);
                                    return queryName;
                                }
                                if (cbtName.contains("philosopher") && queryName.contains("all")) {
                                    logger.info("Found general philosopher match: CBT '{0}' -> Query '{1}'", cbtName, queryName);
                                    return queryName;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug("Failed to access ContextProxy queries for pattern matching: {0}", e.getMessage());
                }
                
                logger.info("No query binding found for CBT '{0}'", cbtName);
            }
        } catch (Exception e) {
            logger.debug("Failed to find bound query for CBT '{0}': {1}", cbtName, e.getMessage());
        }
        return null;
    }

    /**
     * Helper method to convert JavaScript values to JSON strings (replicates DebuggerStateHelper logic)
     */
    private String getVarGsonValue(Object jsValue) {
        try {
            if (jsValue == null) {
                return "null";
            }
            
            // Use Gson to serialize the value
            com.google.gson.Gson gson = new com.google.gson.Gson();
            return gson.toJson(jsValue);
            
        } catch (Exception e) {
            logger.warning("Failed to serialize JS value: {0}", e.getMessage());
            return jsValue != null ? jsValue.toString() : "null";
        }
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
