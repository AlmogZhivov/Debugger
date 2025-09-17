package il.ac.bgu.se.bp.steps;

import il.ac.bgu.se.bp.config.IDECommonTestConfiguration;
import il.ac.bgu.se.bp.rest.request.*;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.rest.response.DebugResponse;
import il.ac.bgu.se.bp.rest.socket.StompPrincipal;
import il.ac.bgu.se.bp.session.ITSessionManager;
import il.ac.bgu.se.bp.socket.state.*;
import il.ac.bgu.se.bp.socket.status.Status;
import il.ac.bgu.se.bp.testService.TestService;
import il.ac.bgu.se.bp.utils.Pair;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static il.ac.bgu.se.bp.code.CodeFilesHelper.getCodeByFileName;
import static il.ac.bgu.se.bp.common.Utils.*;
import static org.junit.Assert.*;

@ContextConfiguration(classes = IDECommonTestConfiguration.class)
public class IDESteps {

    private Map<String, String> userIdsByNames;
    private BooleanResponse lastResponse;
    private DebugResponse lastDebugResponse;
    private Map<String, Map<Integer, Boolean>> breakpointsVerifierPerUser;

    @Autowired
    private TestService testService;

    @Autowired
    private ITSessionManager itSessionManager;

    @Given("user (.*) has connected with userId (.*)")
    public void userUserHasConnectedWithUserIdUserId(String userName, String userId) {
        if (userIdsByNames == null) {
            userIdsByNames = new HashMap<>();
        }
        userIdsByNames.put(userName, userId);
    }

    @Given("(.*) has connected to websocket with (.*)")
    public void userHasConnectedToWebsocketWithSessionIdAndUserId(String userName, String sessionId) {
        testService.subscribeUser(sessionId, new StompPrincipal(getUserIdByName(userName)));
    }


    @When("(.*) asks to debug with filename (.*) and toggleMuteBreakpoints (.*) and toggleMuteSyncPoints (.*) and toggleWaitForExternalEvent (.*) and breakpoints (.*)")
    public void userAsksToDebugWithFilenameAndToggleMuteBreakpointsAndToggleMuteSyncPointsAndBreakpoints(String username, String filename, String toggleMuteBreakpoints,
                                                                                                         String toggleMuteSyncPoints, String toggleWaitForExternalEvent, String breakpoints) {
        itSessionManager.cleanUserMockData(getUserIdByName(username));

        DebugRequest debugRequest = new DebugRequest(getCodeByFileName(filename), strToIntList(breakpoints));
        debugRequest.setSkipBreakpointsToggle(strToBoolean(toggleMuteBreakpoints));
        debugRequest.setSkipSyncStateToggle(strToBoolean(toggleMuteSyncPoints));
        debugRequest.setWaitForExternalEvents(strToBoolean(toggleWaitForExternalEvent));

        lastDebugResponse = testService.debug(getUserIdByName(username), debugRequest);
    }

    @When("(.*) sets sync snapshot, before the first event selection")
    public void userClicksOnSetSyncSnapshotToTheTimeTheFirstEventWasSelected(String username) {
        BPDebuggerState actualDebuggerState = itSessionManager.getUsersLastDebuggerState(getUserIdByName(username));
        LinkedList<Long> timestamps = new LinkedList<>(actualDebuggerState.getEventsHistory().keySet());
        Long snapShotTime = timestamps.get(timestamps.size() - 1);
        setSyncSnapShot(username, "first event", snapShotTime);
    }

    @When("(.*) sets sync snapshot, to the time event (.*) was chosen")
    public void userClicksOnSetSyncSnapshotByEventName(String username, String eventName) {
        BPDebuggerState actualDebuggerState = itSessionManager.getUsersLastDebuggerState(getUserIdByName(username));

        Long snapShotTime = null;
        for (Map.Entry<Long, EventInfo> entry : actualDebuggerState.getEventsHistory().entrySet()) {
            Long time = entry.getKey();
            EventInfo eventInfo = entry.getValue();
            if (eventInfo.getName().equals(eventName)) {
                snapShotTime = time;
                break;
            }
        }

        setSyncSnapShot(username, eventName, snapShotTime);
    }

    private void setSyncSnapShot(String username, String eventName, Long snapShotTime) {
        if (snapShotTime == null) {
            fail(eventName + " was not found, cannot set sync snapshot to the this event was selected");
        }

        SetSyncSnapshotRequest setSyncSnapshotRequest = new SetSyncSnapshotRequest();
        setSyncSnapshotRequest.setSnapShotTime(snapShotTime);
        lastResponse = testService.setSyncSnapshot(getUserIdByName(username), setSyncSnapshotRequest);
    }

    @When("(.*) adds an external event (.*)")
    public void userAddsAnExternalEvent(String username, String externalEvent) {
        ExternalEventRequest externalEventRequest = new ExternalEventRequest();
        externalEventRequest.setAddEvent(true);
        externalEventRequest.setExternalEvent(externalEvent);
        lastResponse = testService.externalEvent(getUserIdByName(username), externalEventRequest);
    }

    @When("(.*) clicks on (.*)")
    public void userClicksOnCommand(String username, String command) {
        itSessionManager.cleanUserMockData(getUserIdByName(username));
        applyCommandByUser(username, command);
    }

    private void applyCommandByUser(String username, String command) {
        String userId = getUserIdByName(username);
        lastResponse = null;
        switch (command) {
            case "continue":
                lastResponse = testService.continueRun(userId);
                break;
            case "next sync":
                lastResponse = testService.nextSync(userId);
                break;
            case "stop":
                lastResponse = testService.stop(userId);
                break;
            case "step into":
                lastResponse = testService.stepInto(userId);
                break;
            case "step over":
                lastResponse = testService.stepOver(userId);
                break;
            case "step out":
                lastResponse = testService.stepOut(userId);
                break;
        }
    }

    @When("(.*) toggles (.*) to (.*)")
    public void userTogglesMuteBreakpoints(String username, String toggleButton, String toggleMode) {
        switch (toggleButton) {
            case "mute breakpoints":
                lastResponse = testService.toggleMuteBreakpoints(getUserIdByName(username), new ToggleBreakpointsRequest(strToBoolean(toggleMode)));
                break;
            case "mute sync states":
                lastResponse = testService.toggleMuteSyncPoints(getUserIdByName(username), new ToggleSyncStatesRequest(strToBoolean(toggleMode)));
                break;
            case "wait for external event":
                lastResponse = testService.toggleWaitForExternal(getUserIdByName(username), new ToggleWaitForExternalRequest(strToBoolean(toggleMode)));
                break;
        }
    }

    @Then("wait until program of user (.*) is over")
    public void waitUntilTheProgramIsOver(String username) {
        waitUntilPredicateSatisfied(() -> itSessionManager.isUserFinishedRunning(getUserIdByName(username)), 500, 3);
    }

    @Then("The response should be (.*) with errorCode (.*)")
    public void theResponseShouldBe(String result, String errorCode) {
        assertBooleanResponse(lastResponse, strToBoolean(result), errorCode);
    }

    @Then("The debug response should be (.*) with errorCode (.*) and breakpoints (.*) for user (.*)")
    public void theDebugResponseShouldBeTrueWithErrorCodeNull(String result, String errorCode, String breakpoints, String username) {
        assertBooleanResponse(lastDebugResponse, strToBoolean(result), errorCode);
        List<Integer> expectedBreakpoints = strToIntList(breakpoints);
        boolean[] actualBreakpoints = lastDebugResponse.getBreakpoints();
        for (int i = 0; i < actualBreakpoints.length; i++) {
            assertEquals(expectedBreakpoints.contains(i), actualBreakpoints[i]);
        }
        expectedBreakpoints.forEach(breakpoint -> assertTrue(actualBreakpoints[breakpoint]));
        initBreakpointsVerifier(username, expectedBreakpoints);
    }

    private void initBreakpointsVerifier(String username, List<Integer> breakpointsList) {
        if (breakpointsVerifierPerUser == null) {
            breakpointsVerifierPerUser = new HashMap<>();
        }

        HashMap<Integer, Boolean> breakpointsVerifier = new HashMap<>();
        breakpointsList.forEach(breakpoint -> breakpointsVerifier.put(breakpoint, false));
        breakpointsVerifierPerUser.put(username, breakpointsVerifier);
    }

    private void assertBooleanResponse(BooleanResponse booleanResponse, boolean expectedResult, String errorCode) {
        assertEquals(expectedResult, booleanResponse.isSuccess());

        if (isNull(errorCode)) {
            assertNull(booleanResponse.getErrorCode());
        }
        else {
            assertNotNull(booleanResponse.getErrorCode());
            assertEquals(errorCode, booleanResponse.getErrorCode().name());
        }
    }

    @Then("verify all breakpoints of user (.*) were reached")
    public void verifyAllBreakpointsWereReached(String username) {
        breakpointsVerifierPerUser.get(username)
                .forEach((breakpoint, isReached) -> assertTrue("did not get to breakpoint " + breakpoint, isReached));
    }

    @Then("verify user (.*) has reached only (.*) breakpoints")
    public void verifyOnlyPartialBreakpointsWereReached(String username, String numOfBreakpointsReachedStr) {
        AtomicInteger actualNumOfBreakpointsReached = new AtomicInteger(0);
        breakpointsVerifierPerUser.get(username).values().forEach(isReached -> {
            if (isReached) {
                actualNumOfBreakpointsReached.incrementAndGet();
            }
        });

        assertEquals(strToInt(numOfBreakpointsReachedStr), actualNumOfBreakpointsReached.get());
    }

    @Then("wait until user (.*) has reached status (.*)")
    public void waitUntilStatusReached(String username, String status) {
        Status requiredStatus = Status.valueOf(status.replaceAll(" ", "_").toUpperCase());
        waitUntilPredicateSatisfied(() -> requiredStatus.equals(itSessionManager.getUsersStatus(getUserIdByName(username))),
                400, 3);
        itSessionManager.removeUsersStatus(getUserIdByName(username));
        sleep(800);         // status is received before the state
    }

    @Then("(.*) should get optional sync state notification wait events (.*), blocked events (.*), requested events (.*), current event (.*), b-threads info list (.*), and events history (.*)")
    public void userShouldGetOptionalSyncStateNotification(String username, String waitEventsStr, String blockedEventsStr,
                                                           String requestedEventsStr, String currentEventStr,
                                                           String bThreadInfoListStr, String eventsHistoryStr) {
        String[] optionalWaitEventsStr = waitEventsStr.split("\\|");
        String[] optionalBlockedEventsStr = blockedEventsStr.split("\\|");
        String[] optionalRequestedEventsStr = requestedEventsStr.split("\\|");
        String[] optionalCurrentEventStr = currentEventStr.split("\\|");
        String[] optionalBThreadInfoListStr = bThreadInfoListStr.split("\\|");
        String[] optionalEventsHistoryStr = eventsHistoryStr.split("\\|");

        int numOfOptions = optionalBlockedEventsStr.length;
        for (int i = 0; i < numOfOptions; i++) {
            try {
                userShouldGetSyncStateNotification(username, optionalWaitEventsStr[i], optionalBlockedEventsStr[i],
                        optionalRequestedEventsStr[i], "", optionalCurrentEventStr[i],
                        optionalBThreadInfoListStr[i], optionalEventsHistoryStr[i]);
                return;
            } catch (Exception ignored) {
            }
        }
        fail("all optional sync states are not matching actual sync state");
    }

    @Then("(.*) should get sync state notification wait events (.*), blocked events (.*), requested events (.*), external events (.*), current event (.*), b-threads info list (.*), and events history (.*)")
    public void userShouldGetSyncStateNotification(String username, String waitEventsStr, String blockedEventsStr,
                                                   String requestedEventsStr, String externalEventsStr, String currentEventStr,
                                                   String bThreadInfoListStr, String eventsHistoryStr) {
        List<String> expectedWaitEvents = strToStringList(waitEventsStr);
        List<String> expectedBlockedEvents = strToStringList(blockedEventsStr);
        List<String> expectedRequestedEvents = strToStringList(requestedEventsStr);
        List<String> expectedExternalEvents = strToStringList(externalEventsStr);
        List<String> expectedEventsHistory = strToStringList(eventsHistoryStr);
        String expectedCurrentEventName = strToString(currentEventStr);
        EventInfo expectedCurrentEvent = expectedCurrentEventName.isEmpty() ? null : new EventInfo(expectedCurrentEventName);
        List<BThreadInfo> expectedBThreadInfoList = strToBThreadInfo(bThreadInfoListStr);

        BPDebuggerState actualDebuggerState = itSessionManager.getUsersLastDebuggerState(getUserIdByName(username));
        EventsStatus actualEventsStatus = actualDebuggerState.getEventsStatus();

        assertEventInfoEquals(expectedWaitEvents, actualEventsStatus.getWait());
        assertEventInfoEquals(expectedBlockedEvents, actualEventsStatus.getBlocked());
        assertEventInfoEquals(expectedRequestedEvents, new LinkedList<>(actualEventsStatus.getRequested()));
        assertEventInfoEquals(expectedExternalEvents, actualEventsStatus.getExternalEvents());
        assertEquals(expectedCurrentEvent, actualEventsStatus.getCurrentEvent());
        assertBTreadInfoList(expectedBThreadInfoList, actualDebuggerState.getbThreadInfoList());
        assertEventsHistory(expectedEventsHistory, actualDebuggerState.getEventsHistory());
    }

    private void assertBTreadInfoList(List<BThreadInfo> expectedBThreadInfoList, List<BThreadInfo> actualBThreadInfoList) {
        assertIntEqual(expectedBThreadInfoList.size(), actualBThreadInfoList.size());
        for (BThreadInfo expectedBThreadInfo : expectedBThreadInfoList) {
            BThreadInfo actualBThreadInfo = getBThreadInfoByName(actualBThreadInfoList, expectedBThreadInfo.getName());
            assertNotNull(actualBThreadInfo);

            throwNotEquals(expectedBThreadInfo.getName(), actualBThreadInfo.getName());
            throwNotEquals(expectedBThreadInfo.getWait(), actualBThreadInfo.getWait());
            throwNotEquals(expectedBThreadInfo.getBlocked(), actualBThreadInfo.getBlocked());
            throwNotEquals(expectedBThreadInfo.getRequested(), actualBThreadInfo.getRequested());
        }
    }

    private void assertEventsHistory(List<String> expectedEventsHistory, SortedMap<Long, EventInfo> actualEventsHistory) {
        Function<Integer, String> getNextEvent = expectedEventsHistory::get;
        AtomicInteger index = new AtomicInteger(0);
        actualEventsHistory.values().forEach(eventInfo -> {
            throwNotEquals(getNextEvent.apply(index.get()), eventInfo.getName());
            index.incrementAndGet();
        });
    }

    private void throwNotEquals(Object expected, Object actual) {
        if ((expected == null && actual == null) || expected.equals(actual)) {
            return;
        }
        String actualString = "\nactual: " + actual.toString();
        String expectedString = "\nexpected: " + expected.toString();
        throw new RuntimeException("not equals" + actualString + expectedString);
    }

    private void assertEventInfoEquals(List<String> expectedEvents, List<EventInfo> actualEventsInfo) {
        assertIntEqual(expectedEvents.size(), actualEventsInfo.size());
        actualEventsInfo.forEach(eventInfo -> {
            if (!expectedEvents.contains(eventInfo.getName())) {
                throw new RuntimeException("unexpected event: " + eventInfo.getName());
            }
        });
    }

    private void assertIntEqual(int expected, int actual) {
        if (expected != actual) {
            throw new RuntimeException("expected != actual\n" + "actual: " + actual + "\nexpected: " + expected);
        }
    }

    @Then("(.*) should get breakpoint notification with BThread (.*), doubles (.*), strings (.*) and breakpoint lines (.*)")
    public void userShouldGetNotificationWithDoubleVariablesAndStringVariables(String username, String bThreads, String doubleVars, String stringVars, String breakpointsStr) {
        BPDebuggerState lastDebuggerState = itSessionManager.getUsersLastDebuggerState(getUserIdByName(username));
        assertNotNull("BPDebuggerState was not received for user: " + username, lastDebuggerState);

        List<Integer> breakpoints = strToIntList(breakpointsStr);
        assertBreakpoints(username, breakpoints, lastDebuggerState, true);

        List<String> bThreadsOfCurrentBreakpoint = getBThreadNamesByBreakpoint(bThreads, lastDebuggerState.getCurrentLineNumber());
        BThreadScope actualEnv = getLastEnvOfMatchingBThread(lastDebuggerState.getbThreadInfoList(), bThreadsOfCurrentBreakpoint);
        assertEnvVariables(actualEnv, lastDebuggerState.getCurrentLineNumber(), doubleVars, stringVars);
    }

    @Then("(.*) should get notification with BThread (.*) on line (.*), envs (.*) and breakpoint lines (.*)")
    public void userShouldGetBreakpointNotificationWithDoubleVariablesAndStringVariables(String username, String bThread, String currentLine,
                                                                                         String envsStr, String breakpointsStr) {
        BPDebuggerState lastDebuggerState = itSessionManager.getUsersLastDebuggerState(getUserIdByName(username));
        assertNotNull("BPDebuggerState was not received for user: " + username, lastDebuggerState);

        List<Integer> breakpoints = strToIntList(breakpointsStr);
        assertBreakpoints(username, breakpoints, lastDebuggerState, false);
        assertEquals(strToInt(currentLine), lastDebuggerState.getCurrentLineNumber().intValue());

        BThreadInfo bThreadInfo = getBThreadInfoByName(lastDebuggerState.getbThreadInfoList(), bThread);
        assertNotNull(bThread + " was not found", bThreadInfo);

        List<Map<String, String>> expectedEnvs = createEnvsMappings(envsStr);
        assertNotNull(expectedEnvs);
        assertEnvs(expectedEnvs, bThreadInfo.getEnv());
    }

    private void assertEnvs(List<Map<String, String>> expectedEnvs, Map<Integer, BThreadScope> actualEnvs) {
        assertEquals(expectedEnvs.size(), actualEnvs.size());

        for (int i = 0; i < expectedEnvs.size(); i++) {
            Map<String, String> actualEnv = actualEnvs.get(i).getVariables();
            Map<String, String> expectedEnv = expectedEnvs.get(i);

            assertEquals(expectedEnv.size(), actualEnv.size());
            expectedEnv.forEach((expectedVariable, expectedValue) -> {
                assertTrue("missing variable: " + expectedVariable, actualEnv.containsKey(expectedVariable));
                assertEquals(expectedValue, actualEnv.get(expectedVariable));
            });
        }
    }

    private void assertEnvVariables(BThreadScope actualEnv, int currentBreakpoint, String doubleVars, String stringVars) {
        List<Pair<String, String>> expectedStringVars = createStringEnvByBreakpoints(stringVars).get(currentBreakpoint);
        if (expectedStringVars != null) {   // test file might not include string vars at this breakpoint
            expectedStringVars.forEach(var -> assertEquals(var.getRight(), strToString(actualEnv.getVariables().get(var.getLeft()))));
        }
        List<Pair<String, Double>> expectedDoubleVars = createDoubleEnvByBreakpoints(doubleVars).get(currentBreakpoint);
        if (expectedDoubleVars != null) {   // test file might not include double vars at this breakpoint
            expectedDoubleVars.forEach(var -> assertEquals(var.getRight(), strToDouble(actualEnv.getVariables().get((var.getLeft()))), 0));
        }
    }

    private void assertBreakpoints(String username, List<Integer> breakpoints, BPDebuggerState bpDebuggerState, boolean isBreakpointStoppage) {
        Integer currentBreakpoint = bpDebuggerState.getCurrentLineNumber();
        assertNotNull("current line is null", currentBreakpoint);
        if (isBreakpointStoppage) {
            assertTrue(breakpoints.contains(currentBreakpoint));
        }
        breakpointsVerifierPerUser.get(username).put(currentBreakpoint, true);

        Boolean[] actualBreakpointsLines = bpDebuggerState.getBreakpoints();
        breakpoints.forEach(breakpointLine -> assertTrue("breakpoint not set on line: " + breakpointLine, actualBreakpointsLines[breakpointLine]));
        int actualNumOfBreakpoints = Arrays.stream(actualBreakpointsLines).reduce(0, (acc, curr) -> acc += curr ? 1 : 0, Integer::sum);
        assertEquals(breakpoints.size(), actualNumOfBreakpoints);
    }

    private String getUserIdByName(String userName) {
        return userIdsByNames.get(userName);
    }
}
