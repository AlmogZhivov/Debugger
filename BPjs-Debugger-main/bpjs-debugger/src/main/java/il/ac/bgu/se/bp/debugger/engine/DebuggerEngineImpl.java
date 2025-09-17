package il.ac.bgu.se.bp.debugger.engine;

import il.ac.bgu.cs.bp.bpjs.BPjs;
import il.ac.bgu.cs.bp.bpjs.model.BProgramSyncSnapshot;
import il.ac.bgu.se.bp.debugger.DebuggerLevel;
import il.ac.bgu.se.bp.debugger.RunnerState;
import il.ac.bgu.se.bp.debugger.commands.DebuggerCommand;
import il.ac.bgu.se.bp.debugger.engine.dim.DimHelper;
import il.ac.bgu.se.bp.debugger.engine.dim.DimHelperImpl;
import il.ac.bgu.se.bp.debugger.engine.events.BPStateEvent;
import il.ac.bgu.se.bp.debugger.engine.events.ProgramStatusEvent;
import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import il.ac.bgu.se.bp.socket.status.Status;
import il.ac.bgu.se.bp.utils.DebuggerStateHelper;
import il.ac.bgu.se.bp.utils.DebuggerStopException;
import il.ac.bgu.se.bp.utils.logger.Logger;
import il.ac.bgu.se.bp.utils.observer.BPEvent;
import il.ac.bgu.se.bp.utils.observer.BPEventPublisherImpl;
import il.ac.bgu.se.bp.utils.observer.Publisher;
import il.ac.bgu.se.bp.utils.observer.Subscriber;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.tools.debugger.Dim;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import static il.ac.bgu.se.bp.utils.ProgramStatusHelper.getRunStatusByDebuggerLevel;

public class DebuggerEngineImpl implements DebuggerEngine<BProgramSyncSnapshot> {
    private final String filename;
    private final RunnerState state;
    private final DebuggerStateHelper debuggerStateHelper;
    private final String debuggerId;
    private final Logger logger;
    private final ExecutorService execSvc;

    private DimHelper dimHelper;
    private Dim.ContextData lastContextData = null;
    private volatile boolean isRunning;
    private volatile boolean areBreakpointsMuted = false;
    private BProgramSyncSnapshot syncSnapshot = null;
    private DebuggerLevel debuggerLevel;

    private final BlockingQueue<DebuggerCommand> debuggerCommands = new ArrayBlockingQueue<>(1);
    private Publisher<BPEvent> publisher = new BPEventPublisherImpl();

    public DebuggerEngineImpl(String debuggerId, String filename, RunnerState state,
                              DebuggerStateHelper debuggerStateHelper, String debuggerThreadId) {
        this.filename = filename;
        this.state = state;
        this.debuggerStateHelper = debuggerStateHelper;
        this.debuggerId = debuggerId;
        this.logger = new Logger(DebuggerEngineImpl.class, debuggerId);
        this.execSvc = BPjs.getExecutorServiceMaker().makeWithName(debuggerThreadId);

        initDim();
        setIsRunning(true);
    }

    private void initDim() {
        dimHelper = new DimHelperImpl();
        dimHelper.setGuiCallback(this);
        dimHelper.attachTo(ContextFactory.getGlobal());
    }

    public void changeDebuggerLevel(DebuggerLevel debuggerLevel) {
        this.debuggerLevel = debuggerLevel;
    }

    public void setupBreakpoints(Map<Integer, Boolean> breakpoints) throws IllegalArgumentException {
        if (breakpoints == null) {
            return;
        }
        breakpoints.forEach(this::setBreakpoint);
    }

    @Override
    public void updateSourceText(Dim.SourceInfo sourceInfo) {
    }

    @Override
    public void enterInterrupt(Dim.StackFrame stackFrame, String s, String s1) {
        verifyState();
        state.setDebuggerState(RunnerState.State.JS_DEBUG);
        lastContextData = stackFrame.contextData();
        logger.debug("Get state from enterInterrupt, line number: {0}", stackFrame.getLineNumber());
        if (isMuteBreakpoints()) {
            continueRun();
        }
        else {
            notifySubscribers(new ProgramStatusEvent(debuggerId, Status.BREAKPOINT));
            onStateChanged();
        }
    }

    @Override
    public boolean isGuiEventThread() {
        return true;
    }

    @Override
    public void dispatchNextGuiEvent() {
        verifyState();
        try {
            if (!debuggerStateHelper.getLastState().equals(debuggerStateHelper.peekNextState(syncSnapshot, state, lastContextData,dimHelper.getSourceInfo(filename)))) {
                logger.info("Getting state from dispatchNextGuiEvent");
                onStateChanged();
            }
            if (isRunning()) {
                DebuggerCommand debuggerCommand = debuggerCommands.take();
                logger.info("applying command " + debuggerCommand.toString());
                debuggerCommand.applyCommand(this);
            }
        } catch (Exception e) {
            if (isRunning()) {
                logger.error("failed on dispatchNextGuiEvent", e);
            }
        }
    }

    private void verifyState() {
        if (!isRunning()) {
            throw new DebuggerStopException("debugger is not running [probably force stop scenario]");
        }
    }

    @Override
    public void addCommand(DebuggerCommand command) throws Exception {
        debuggerCommands.add(command);
    }

    public synchronized boolean isRunning() {
        return isRunning;
    }

    private synchronized void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    @Override
    public synchronized void toggleMuteBreakpoints(boolean toggleBreakPointStatus) {
        this.areBreakpointsMuted = toggleBreakPointStatus;
    }

    @Override
    public void stop() {
        logger.info("stopping debugger engine");
        dimHelper.stop();
        execSvc.shutdownNow();
        setIsRunning(false);
    }

    @Override
    public void stepOut() {
        dimHelper.setReturnValue(Dim.STEP_OUT);
    }

    @Override
    public void stepInto() {
        dimHelper.setReturnValue(Dim.STEP_INTO);
//        dim.setBreakOnEnter(true); //possible bug because BP
    }

    @Override
    public void stepOver() {
        dimHelper.setReturnValue(Dim.STEP_OVER);
    }

    @Override
    public void continueRun() {
        notifySubscribers(new ProgramStatusEvent(debuggerId, getRunStatusByDebuggerLevel(debuggerLevel)));
        dimHelper.go();
    }

    @Override
    public boolean isBreakpointAllowed(int lineNumber) {
        return dimHelper.isBreakpointAllowed(lineNumber, filename);
    }

    @Override
    public void setBreakpoint(int lineNumber, boolean stopOnBreakpoint) throws IllegalArgumentException {
        if(isBreakpointAllowed(lineNumber))
            dimHelper.setBreakpoint(lineNumber, stopOnBreakpoint, filename);
    }

    @Override
    public void setSyncSnapshot(BProgramSyncSnapshot syncSnapshot) {
        this.syncSnapshot = syncSnapshot;
    }

    @Override
    public void onStateChanged() {
        try {
            BPDebuggerState newState = debuggerStateHelper.generateDebuggerState(syncSnapshot, state, lastContextData, dimHelper.getSourceInfo(filename));
            execSvc.submit(() -> notifySubscribers(new BPStateEvent(debuggerId, newState))).get();
        } catch (Exception e) {
            logger.error("onStateChanged: failed e: {0}", e, e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public boolean[] getBreakpoints() {
        return debuggerStateHelper.getBreakpoints(dimHelper.getSourceInfo(filename));
    }

    @Override
    public boolean isMuteBreakpoints() {
        return areBreakpointsMuted;
    }

    @Override
    public void getState() {
        onStateChanged();
    }

    @Override
    public void subscribe(Subscriber subscriber) {
        publisher.subscribe(subscriber);
    }

    @Override
    public void unsubscribe(Subscriber subscriber) {
        publisher.unsubscribe(subscriber);
    }

    @Override
    public void notifySubscribers(BPEvent event) {
        publisher.notifySubscribers(event);
    }
}