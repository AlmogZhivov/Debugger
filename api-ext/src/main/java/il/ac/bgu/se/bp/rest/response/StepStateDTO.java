package il.ac.bgu.se.bp.rest.response;

import java.util.List;
import java.util.Map;

/**
 * Lightweight DTO for step operations that contains only primitive types and strings.
 * This avoids circular reference issues during Gson serialization.
 */
public class StepStateDTO {
    private List<String> activeBThreads;
    private List<String> requestedEvents;
    private List<String> blockedEvents;
    private List<String> waitEvents;
    private Map<String, String> globalVariables;
    private Map<String, String> eventsHistory;
    private String currentRunningBT;
    private String currentLineNumber;
    private boolean skipBreakpoints;
    private boolean skipSyncPoints;
    private boolean waitForExternalEvents;
    private String[] breakpoints;
    
    // COBP Context Information
    private Map<String, String> contextStore;
    private List<String> contextEntities;
    private String currentContext;
    private Map<String, String> contextVariables;
    
    // Detailed B-Thread Information
    private List<BThreadInfoDTO> bThreadInfoList;

    public StepStateDTO() {
    }

    public List<String> getActiveBThreads() {
        return activeBThreads;
    }

    public void setActiveBThreads(List<String> activeBThreads) {
        this.activeBThreads = activeBThreads;
    }

    public List<String> getRequestedEvents() {
        return requestedEvents;
    }

    public void setRequestedEvents(List<String> requestedEvents) {
        this.requestedEvents = requestedEvents;
    }

    public List<String> getBlockedEvents() {
        return blockedEvents;
    }

    public void setBlockedEvents(List<String> blockedEvents) {
        this.blockedEvents = blockedEvents;
    }

    public List<String> getWaitEvents() {
        return waitEvents;
    }

    public void setWaitEvents(List<String> waitEvents) {
        this.waitEvents = waitEvents;
    }

    public Map<String, String> getGlobalVariables() {
        return globalVariables;
    }

    public void setGlobalVariables(Map<String, String> globalVariables) {
        this.globalVariables = globalVariables;
    }

    public Map<String, String> getEventsHistory() {
        return eventsHistory;
    }

    public void setEventsHistory(Map<String, String> eventsHistory) {
        this.eventsHistory = eventsHistory;
    }

    public String getCurrentRunningBT() {
        return currentRunningBT;
    }

    public void setCurrentRunningBT(String currentRunningBT) {
        this.currentRunningBT = currentRunningBT;
    }

    public String getCurrentLineNumber() {
        return currentLineNumber;
    }

    public void setCurrentLineNumber(String currentLineNumber) {
        this.currentLineNumber = currentLineNumber;
    }

    public boolean isSkipBreakpoints() {
        return skipBreakpoints;
    }

    public void setSkipBreakpoints(boolean skipBreakpoints) {
        this.skipBreakpoints = skipBreakpoints;
    }

    public boolean isSkipSyncPoints() {
        return skipSyncPoints;
    }

    public void setSkipSyncPoints(boolean skipSyncPoints) {
        this.skipSyncPoints = skipSyncPoints;
    }

    public boolean isWaitForExternalEvents() {
        return waitForExternalEvents;
    }

    public void setWaitForExternalEvents(boolean waitForExternalEvents) {
        this.waitForExternalEvents = waitForExternalEvents;
    }

    public String[] getBreakpoints() {
        return breakpoints;
    }

    public void setBreakpoints(String[] breakpoints) {
        this.breakpoints = breakpoints;
    }

    // COBP Context getters and setters
    public Map<String, String> getContextStore() {
        return contextStore;
    }

    public void setContextStore(Map<String, String> contextStore) {
        this.contextStore = contextStore;
    }

    public List<String> getContextEntities() {
        return contextEntities;
    }

    public void setContextEntities(List<String> contextEntities) {
        this.contextEntities = contextEntities;
    }

    public String getCurrentContext() {
        return currentContext;
    }

    public void setCurrentContext(String currentContext) {
        this.currentContext = currentContext;
    }

    public Map<String, String> getContextVariables() {
        return contextVariables;
    }

    public void setContextVariables(Map<String, String> contextVariables) {
        this.contextVariables = contextVariables;
    }

    // B-Thread Info getters and setters
    public List<BThreadInfoDTO> getbThreadInfoList() {
        return bThreadInfoList;
    }

    public void setbThreadInfoList(List<BThreadInfoDTO> bThreadInfoList) {
        this.bThreadInfoList = bThreadInfoList;
    }

    @Override
    public String toString() {
        return "StepStateDTO{" +
                "activeBThreads=" + activeBThreads +
                ", requestedEvents=" + requestedEvents +
                ", blockedEvents=" + blockedEvents +
                ", waitEvents=" + waitEvents +
                ", globalVariables=" + globalVariables +
                ", eventsHistory=" + eventsHistory +
                ", currentRunningBT='" + currentRunningBT + '\'' +
                ", currentLineNumber='" + currentLineNumber + '\'' +
                ", skipBreakpoints=" + skipBreakpoints +
                ", skipSyncPoints=" + skipSyncPoints +
                ", waitForExternalEvents=" + waitForExternalEvents +
                ", breakpoints=" + java.util.Arrays.toString(breakpoints) +
                ", contextStore=" + contextStore +
                ", contextEntities=" + contextEntities +
                ", currentContext='" + currentContext + '\'' +
                ", contextVariables=" + contextVariables +
                ", bThreadInfoList=" + bThreadInfoList +
                '}';
    }
}
