package il.ac.bgu.se.bp.service;

import il.ac.bgu.se.bp.rest.request.*;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.DebugResponse;
import il.ac.bgu.se.bp.rest.response.EventsHistoryResponse;
import il.ac.bgu.se.bp.rest.response.StepResponse;
import il.ac.bgu.se.bp.rest.response.SyncSnapshot;

/**
 * Service interface for COBP (Context-Oriented Behavior Programming) IDE operations.
 * This service provides the same interface as BPjsIDEService but uses COBP-specific
 * debugger implementations.
 */
public interface COBPIDEService {

    void subscribeUser(String sessionId, String userId);

    BooleanResponse run(RunRequest runRequest, String userId);
    DebugResponse debug(DebugRequest debugRequest, String userId);

    BooleanResponse setBreakpoint(String userId, SetBreakpointRequest setBreakpointRequest);
    BooleanResponse toggleMuteBreakpoints(String userId, ToggleBreakpointsRequest toggleBreakPointStatus);
    BooleanResponse toggleWaitForExternal(String userId, ToggleWaitForExternalRequest toggleWaitForExternalRequest);
    BooleanResponse toggleMuteSyncPoints(String userId, ToggleSyncStatesRequest toggleMuteSyncPoints);

    BooleanResponse stop(String userId);
    BooleanResponse stepOut(String userId);
    BooleanResponse stepInto(String userId);
    BooleanResponse stepOver(String userId);
    BooleanResponse continueRun(String userId);

    BooleanResponse nextSync(String userId);

    BooleanResponse externalEvent(String userId, ExternalEventRequest externalEventRequest);
    EventsHistoryResponse getEventsHistory(String userId, int from, int to);

    BooleanResponse setSyncSnapshot(String userId, SetSyncSnapshotRequest setSyncSnapshotRequest);
    SyncSnapshot exportSyncSnapshot(String userId);
    BooleanResponse importSyncSnapshot(String userId, ImportSyncSnapshotRequest importSyncSnapshotRequest);

    // New methods that return StepResponse with debugger state
    StepResponse stepIntoWithState(String userId);
    StepResponse stepOverWithState(String userId);
    StepResponse stepOutWithState(String userId);
    StepResponse nextSyncWithState(String userId);
}
