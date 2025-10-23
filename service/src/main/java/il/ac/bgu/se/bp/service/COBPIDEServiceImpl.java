package il.ac.bgu.se.bp.service;

import il.ac.bgu.se.bp.debugger.BPJsDebugger;
import il.ac.bgu.se.bp.debugger.DebuggerLevel;
import il.ac.bgu.se.bp.debugger.manage.DebuggerFactory;
import il.ac.bgu.se.bp.error.ErrorCode;
import il.ac.bgu.se.bp.rest.request.*;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.DebugResponse;
import il.ac.bgu.se.bp.rest.response.EventsHistoryResponse;
import il.ac.bgu.se.bp.rest.response.StepResponse;
import il.ac.bgu.se.bp.rest.response.SyncSnapshot;
import il.ac.bgu.se.bp.service.code.SourceCodeHelper;
import il.ac.bgu.se.bp.service.manage.SessionHandler;
import il.ac.bgu.se.bp.utils.logger.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.thymeleaf.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of COBP IDE Service that provides COBP-specific debugging capabilities.
 * This service uses the COBP debugger factory to create COBP debugger instances.
 */
@Service("cobpIDEService")
public class COBPIDEServiceImpl implements COBPIDEService {

    private static final Logger logger = new Logger(COBPIDEServiceImpl.class);

    @Autowired
    private SessionHandler<BPJsDebugger<BooleanResponse>> sessionHandler;

    @Autowired
    private SourceCodeHelper sourceCodeHelper;

    @Autowired
    @Qualifier("cobpDebuggerFactory")
    private DebuggerFactory<BooleanResponse> cobpDebuggerFactory;

    // Note: ContextFactory.initGlobal() is already called by BPjsIDEServiceImpl
    // No need to initialize it again here to avoid IllegalStateException

    @Override
    public void subscribeUser(String sessionId, String userId) {
        System.out.println("Received COBP message from {1} with sessionId {2}" + ",," + userId + "," + sessionId);
        sessionHandler.addUser(sessionId, userId);
    }

    @Override
    public BooleanResponse run(RunRequest runRequest, String userId) {
        if (!validateRequest(runRequest)) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }
        logger.info("received COBP run request for user: {0}", userId);
        String filename = sourceCodeHelper.createCodeFile(runRequest.getSourceCode());
        if (StringUtils.isEmpty(filename)) {
            return createErrorResponse(ErrorCode.INVALID_SOURCE_CODE);
        }

        BPJsDebugger<BooleanResponse> cobpProgramDebugger = cobpDebuggerFactory.getBPJsDebugger(userId, filename, DebuggerLevel.LIGHT);
        cobpProgramDebugger.subscribe(sessionHandler);
        sessionHandler.addNewRunExecution(userId, cobpProgramDebugger, filename);
        sessionHandler.updateLastOperationTime(userId);

        return cobpProgramDebugger.startSync(new HashMap<>(), true, true, runRequest.isWaitForExternalEvents());
    }

    @Override
    public DebugResponse debug(DebugRequest debugRequest, String userId) {
        if (!validateRequest(debugRequest)) {
            return new DebugResponse(createErrorResponse(ErrorCode.INVALID_REQUEST));
        }

        if (!sessionHandler.validateUserId(userId)) {
            return new DebugResponse(createErrorResponse(ErrorCode.UNKNOWN_USER));
        }

        String filename = sourceCodeHelper.createCodeFile(debugRequest.getSourceCode());
        if (StringUtils.isEmpty(filename)) {
            return new DebugResponse(createErrorResponse(ErrorCode.INVALID_SOURCE_CODE));
        }

        logger.info("received COBP debug request for user: {0}", userId);
        return handleNewDebugRequest(debugRequest, userId, filename);
    }

    private DebugResponse handleNewDebugRequest(DebugRequest debugRequest, String userId, String filename) {
        BPJsDebugger<BooleanResponse> cobpProgramDebugger = cobpDebuggerFactory.getBPJsDebugger(userId, filename, DebuggerLevel.NORMAL);
        cobpProgramDebugger.subscribe(sessionHandler);

        sessionHandler.addNewDebugExecution(userId, cobpProgramDebugger, filename);
        sessionHandler.updateLastOperationTime(userId);

        Map<Integer, Boolean> breakpointsMap = debugRequest.getBreakpoints()
                .stream()
                .collect(Collectors.toMap(Function.identity(), b -> Boolean.TRUE));

        return cobpProgramDebugger.startSync(breakpointsMap, debugRequest.isSkipSyncStateToggle(), debugRequest.isSkipBreakpointsToggle(), debugRequest.isWaitForExternalEvents());
    }

    @Override
    public BooleanResponse setBreakpoint(String userId, SetBreakpointRequest setBreakpointRequest) {
        if (!validateRequest(setBreakpointRequest)) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.setBreakpoint(setBreakpointRequest.getLineNumber(), setBreakpointRequest.isStopOnBreakpoint());
    }

    @Override
    public BooleanResponse toggleMuteBreakpoints(String userId, ToggleBreakpointsRequest toggleBreakPointStatus) {
        if (!validateRequest(toggleBreakPointStatus)) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.toggleMuteBreakpoints(toggleBreakPointStatus.isSkipBreakpoints());
    }

    @Override
    public BooleanResponse toggleWaitForExternal(String userId, ToggleWaitForExternalRequest toggleWaitForExternalRequest) {
        if (!validateRequest(toggleWaitForExternalRequest)) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.toggleWaitForExternalEvents(toggleWaitForExternalRequest.isWaitForExternal());
    }

    @Override
    public BooleanResponse toggleMuteSyncPoints(String userId, ToggleSyncStatesRequest toggleMuteSyncPoints) {
        if (!validateRequest(toggleMuteSyncPoints)) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.toggleMuteSyncPoints(toggleMuteSyncPoints.isSkipSyncStates());
    }

    @Override
    public BooleanResponse stop(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.stop();
    }

    @Override
    public BooleanResponse stepOut(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.stepOut();
    }
    
    public StepResponse stepOutWithStepResponse(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        // Cast to COBPDebuggerImpl to access the new method
        if (cobpDebugger instanceof il.ac.bgu.se.bp.execution.COBPDebuggerImpl) {
            return ((il.ac.bgu.se.bp.execution.COBPDebuggerImpl) cobpDebugger).stepOutWithStepResponse();
        } else {
            return new StepResponse(false, "NOT_COBP_DEBUGGER", null);
        }
    }

    @Override
    public BooleanResponse stepInto(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.stepInto();
    }
    
    public StepResponse stepIntoWithStepResponse(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        // Cast to COBPDebuggerImpl to access the new method
        if (cobpDebugger instanceof il.ac.bgu.se.bp.execution.COBPDebuggerImpl) {
            return ((il.ac.bgu.se.bp.execution.COBPDebuggerImpl) cobpDebugger).stepIntoWithStepResponse();
        } else {
            return new StepResponse(false, "NOT_COBP_DEBUGGER", null);
        }
    }

    @Override
    public BooleanResponse stepOver(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.stepOver();
    }
    
    public StepResponse stepOverWithStepResponse(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        // Cast to COBPDebuggerImpl to access the new method
        if (cobpDebugger instanceof il.ac.bgu.se.bp.execution.COBPDebuggerImpl) {
            return ((il.ac.bgu.se.bp.execution.COBPDebuggerImpl) cobpDebugger).stepOverWithStepResponse();
        } else {
            return new StepResponse(false, "NOT_COBP_DEBUGGER", null);
        }
    }

    @Override
    public BooleanResponse continueRun(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.continueRun();
    }

    @Override
    public BooleanResponse nextSync(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.nextSync();
    }
    
    public StepResponse nextSyncWithStepResponse(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        // Cast to COBPDebuggerImpl to access the new method
        if (cobpDebugger instanceof il.ac.bgu.se.bp.execution.COBPDebuggerImpl) {
            return ((il.ac.bgu.se.bp.execution.COBPDebuggerImpl) cobpDebugger).nextSyncWithStepResponse();
        } else {
            return new StepResponse(false, "NOT_COBP_DEBUGGER", null);
        }
    }

    @Override
    public BooleanResponse externalEvent(String userId, ExternalEventRequest externalEventRequest) {
        if (!validateRequest(externalEventRequest)) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.addExternalEvent(externalEventRequest.getExternalEvent());
    }

    @Override
    public EventsHistoryResponse getEventsHistory(String userId, int from, int to) {
        if (!sessionHandler.validateUserId(userId)) {
            return new EventsHistoryResponse(null);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return new EventsHistoryResponse(null);
        }

        return new EventsHistoryResponse(cobpDebugger.getEventsHistory(from, to));
    }

    @Override
    public BooleanResponse setSyncSnapshot(String userId, SetSyncSnapshotRequest setSyncSnapshotRequest) {
        if (!validateRequest(setSyncSnapshotRequest)) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.setSyncSnapshot(setSyncSnapshotRequest.getSnapShotTime());
    }

    @Override
    public SyncSnapshot exportSyncSnapshot(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return new SyncSnapshot(null, null);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return new SyncSnapshot(null, null);
        }

        byte[] syncSnapshotBytes = cobpDebugger.getSyncSnapshot();
        if (syncSnapshotBytes == null) {
            return new SyncSnapshot(null, null);
        }

        return new SyncSnapshot(null, syncSnapshotBytes);
    }

    @Override
    public BooleanResponse importSyncSnapshot(String userId, ImportSyncSnapshotRequest importSyncSnapshotRequest) {
        if (!validateRequest(importSyncSnapshotRequest)) {
            return createErrorResponse(ErrorCode.INVALID_REQUEST);
        }

        if (!sessionHandler.validateUserId(userId)) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return createErrorResponse(ErrorCode.UNKNOWN_USER);
        }

        return cobpDebugger.setSyncSnapshot(importSyncSnapshotRequest.getSyncSnapshot());
    }

    private boolean validateRequest(Object request) {
        return request != null;
    }

    private BooleanResponse createErrorResponse(ErrorCode errorCode) {
        return new BooleanResponse(false, errorCode);
    }

    // New methods that return StepResponse with debugger state
    @Override
    public StepResponse stepIntoWithState(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        // Debug: Log the actual debugger type
        logger.info("Debugger type: {0}", cobpDebugger.getClass().getName());
        logger.info("Is COBPDebuggerImpl: {0}", cobpDebugger instanceof il.ac.bgu.se.bp.execution.COBPDebuggerImpl);

        // Cast to COBPDebuggerImpl to access the new method
        if (cobpDebugger instanceof il.ac.bgu.se.bp.execution.COBPDebuggerImpl) {
            return ((il.ac.bgu.se.bp.execution.COBPDebuggerImpl) cobpDebugger).stepIntoWithState();
        } else {
            return new StepResponse(false, "NOT_COBP_DEBUGGER - Actual type: " + cobpDebugger.getClass().getName(), null);
        }
    }

    @Override
    public StepResponse stepOverWithState(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        // Cast to COBPDebuggerImpl to access the new method
        if (cobpDebugger instanceof il.ac.bgu.se.bp.execution.COBPDebuggerImpl) {
            return ((il.ac.bgu.se.bp.execution.COBPDebuggerImpl) cobpDebugger).stepOverWithState();
        } else {
            return new StepResponse(false, "NOT_COBP_DEBUGGER", null);
        }
    }

    @Override
    public StepResponse stepOutWithState(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        // Cast to COBPDebuggerImpl to access the new method
        if (cobpDebugger instanceof il.ac.bgu.se.bp.execution.COBPDebuggerImpl) {
            return ((il.ac.bgu.se.bp.execution.COBPDebuggerImpl) cobpDebugger).stepOutWithState();
        } else {
            return new StepResponse(false, "NOT_COBP_DEBUGGER", null);
        }
    }

    @Override
    public StepResponse nextSyncWithState(String userId) {
        if (!sessionHandler.validateUserId(userId)) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        BPJsDebugger<BooleanResponse> cobpDebugger = sessionHandler.getBPjsDebuggerByUser(userId);
        if (cobpDebugger == null) {
            return new StepResponse(false, ErrorCode.UNKNOWN_USER.toString(), null);
        }

        // Cast to COBPDebuggerImpl to access the new method
        if (cobpDebugger instanceof il.ac.bgu.se.bp.execution.COBPDebuggerImpl) {
            return ((il.ac.bgu.se.bp.execution.COBPDebuggerImpl) cobpDebugger).nextSyncWithState();
        } else {
            return new StepResponse(false, "NOT_COBP_DEBUGGER", null);
        }
    }
}
