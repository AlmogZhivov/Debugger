package il.ac.bgu.se.bp.rest;

import il.ac.bgu.se.bp.rest.controller.BPjsIDERestController;
import il.ac.bgu.se.bp.rest.request.*;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.DebugResponse;
import il.ac.bgu.se.bp.rest.response.EventsHistoryResponse;
import il.ac.bgu.se.bp.rest.response.SyncSnapshot;
import il.ac.bgu.se.bp.service.COBPIDEService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

import static il.ac.bgu.se.bp.rest.utils.Constants.SIMP_SESSION_ID;
import static il.ac.bgu.se.bp.rest.utils.Endpoints.*;

/**
 * REST Controller for COBP (Context-Oriented Behavior Programming) debugging.
 * This controller provides the same endpoints as the regular BPjs controller
 * but uses the COBP service to handle COBP-specific debugging operations.
 */
@Controller
@RequestMapping("/cobp")
public class COBPIDERestControllerImpl implements BPjsIDERestController {

    @Autowired
    @Qualifier("cobpIDEService")
    private COBPIDEService cobpIDEService;

    @MessageMapping("/cobp/subscribe")
    public @ResponseBody
    void subscribeUser(@Header(SIMP_SESSION_ID) String sessionId, Principal principal) {
        System.out.println("COBP userId: " + principal.getName());
        cobpIDEService.subscribeUser(sessionId, principal.getName());
    }

    @Override
    @RequestMapping(value = RUN, method = RequestMethod.POST)
    public @ResponseBody
    BooleanResponse run(@RequestHeader("userId") String userId, @RequestBody RunRequest code) {
        return cobpIDEService.run(code, userId);
    }

    @Override
    @RequestMapping(value = DEBUG, method = RequestMethod.POST)
    public @ResponseBody
    DebugResponse debug(@RequestHeader("userId") String userId, @RequestBody DebugRequest code) {
        return cobpIDEService.debug(code, userId);
    }

    @Override
    @RequestMapping(value = BREAKPOINT, method = RequestMethod.POST)
    public @ResponseBody
    BooleanResponse setBreakpoint(@RequestHeader("userId") String userId,
                                  @RequestBody SetBreakpointRequest setBreakpointRequest) {
        return cobpIDEService.setBreakpoint(userId, setBreakpointRequest);
    }

    @Override
    @RequestMapping(value = BREAKPOINT, method = RequestMethod.PUT)
    public @ResponseBody
    BooleanResponse toggleMuteBreakpoints(@RequestHeader("userId") String userId,
                                          @RequestBody ToggleBreakpointsRequest toggleBreakpointsRequest) {
        return cobpIDEService.toggleMuteBreakpoints(userId, toggleBreakpointsRequest);
    }

    @Override
    @RequestMapping
    public @ResponseBody
    BooleanResponse toggleWaitForExternal(@RequestHeader("userId") String userId,
                                          @RequestBody ToggleWaitForExternalRequest toggleWaitForExternalRequest) {
        return cobpIDEService.toggleWaitForExternal(userId, toggleWaitForExternalRequest);
    }

    @Override
    @RequestMapping(value = SYNC_STATES, method = RequestMethod.PUT)
    public @ResponseBody
    BooleanResponse toggleMuteSyncPoints(@RequestHeader("userId") String userId,
                                         @RequestBody ToggleSyncStatesRequest toggleMuteSyncPoints) {
        return cobpIDEService.toggleMuteSyncPoints(userId, toggleMuteSyncPoints);
    }

    @Override
    @RequestMapping(value = STOP, method = RequestMethod.GET)
    public @ResponseBody
    BooleanResponse stop(@RequestHeader("userId") String userId) {
        return cobpIDEService.stop(userId);
    }

    @Override
    @RequestMapping(value = STEP_OUT, method = RequestMethod.GET)
    public @ResponseBody
    BooleanResponse stepOut(@RequestHeader("userId") String userId) {
        return cobpIDEService.stepOut(userId);
    }

    @Override
    @RequestMapping(value = STEP_INTO, method = RequestMethod.GET)
    public @ResponseBody
    BooleanResponse stepInto(@RequestHeader("userId") String userId) {
        return cobpIDEService.stepInto(userId);
    }

    @Override
    @RequestMapping(value = STEP_OVER, method = RequestMethod.GET)
    public @ResponseBody
    BooleanResponse stepOver(@RequestHeader("userId") String userId) {
        return cobpIDEService.stepOver(userId);
    }

    @Override
    @RequestMapping(value = CONTINUE, method = RequestMethod.GET)
    public @ResponseBody
    BooleanResponse continueRun(@RequestHeader("userId") String userId) {
        return cobpIDEService.continueRun(userId);
    }

    @Override
    @RequestMapping(value = NEXT_SYNC, method = RequestMethod.GET)
    public @ResponseBody
    BooleanResponse nextSync(@RequestHeader("userId") String userId) {
        return cobpIDEService.nextSync(userId);
    }

    @Override
    @RequestMapping(value = EXTERNAL_EVENT, method = RequestMethod.POST)
    public @ResponseBody
    BooleanResponse externalEvent(@RequestHeader("userId") String userId,
                                  @RequestBody ExternalEventRequest externalEventRequest) {
        return cobpIDEService.externalEvent(userId, externalEventRequest);
    }

    @Override
    @RequestMapping(value = EVENTS, method = RequestMethod.GET)
    public @ResponseBody
    EventsHistoryResponse getEventsHistory(@RequestHeader("userId") String userId,
                                           @RequestParam(name = "from") int from,
                                           @RequestParam(name = "to") int to) {
        return cobpIDEService.getEventsHistory(userId, from, to);
    }

    @Override
    @RequestMapping(value = SYNC_SNAPSHOT, method = RequestMethod.PUT)
    public @ResponseBody
    BooleanResponse setSyncSnapshot(@RequestHeader("userId") String userId,
                                    @RequestBody SetSyncSnapshotRequest setSyncSnapshotRequest) {
        return cobpIDEService.setSyncSnapshot(userId, setSyncSnapshotRequest);
    }

    @Override
    @RequestMapping(value = SYNC_SNAPSHOT, method = RequestMethod.GET)
    public @ResponseBody
    SyncSnapshot exportSyncSnapshot(@RequestHeader("userId") String userId) {
        return cobpIDEService.exportSyncSnapshot(userId);
    }

    @Override
    @RequestMapping(value = SYNC_SNAPSHOT, method = RequestMethod.POST)
    public @ResponseBody
    BooleanResponse importSyncSnapshot(@RequestHeader("userId") String userId,
                                       @RequestBody ImportSyncSnapshotRequest importSyncSnapshotRequest) {
        return cobpIDEService.importSyncSnapshot(userId, importSyncSnapshotRequest);
    }
}
