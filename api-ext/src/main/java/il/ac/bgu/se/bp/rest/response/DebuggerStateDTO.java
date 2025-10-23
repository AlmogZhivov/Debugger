package il.ac.bgu.se.bp.rest.response;

import java.util.List;
import java.util.Map;

/**
 * Serialization-safe DTO for debugger state.
 * Contains only primitive types and collections to avoid circular references during Gson serialization.
 */
public class DebuggerStateDTO {
    private String currentRunningBT;
    private Integer currentLineNumber;
    private List<BThreadInfoDTO> bThreadInfoList;
    private EventsStatusDTO eventsStatus;
    private Map<String, String> globalEnv;
    private Map<Long, String> eventsHistory; // Simplified to just event names
    private Boolean[] breakpoints;
    private DebuggerConfigsDTO debuggerConfigs;

    public DebuggerStateDTO() {
    }

    public String getCurrentRunningBT() {
        return currentRunningBT;
    }

    public void setCurrentRunningBT(String currentRunningBT) {
        this.currentRunningBT = currentRunningBT;
    }

    public Integer getCurrentLineNumber() {
        return currentLineNumber;
    }

    public void setCurrentLineNumber(Integer currentLineNumber) {
        this.currentLineNumber = currentLineNumber;
    }

    public List<BThreadInfoDTO> getbThreadInfoList() {
        return bThreadInfoList;
    }

    public void setbThreadInfoList(List<BThreadInfoDTO> bThreadInfoList) {
        this.bThreadInfoList = bThreadInfoList;
    }

    public EventsStatusDTO getEventsStatus() {
        return eventsStatus;
    }

    public void setEventsStatus(EventsStatusDTO eventsStatus) {
        this.eventsStatus = eventsStatus;
    }

    public Map<String, String> getGlobalEnv() {
        return globalEnv;
    }

    public void setGlobalEnv(Map<String, String> globalEnv) {
        this.globalEnv = globalEnv;
    }

    public Map<Long, String> getEventsHistory() {
        return eventsHistory;
    }

    public void setEventsHistory(Map<Long, String> eventsHistory) {
        this.eventsHistory = eventsHistory;
    }

    public Boolean[] getBreakpoints() {
        return breakpoints;
    }

    public void setBreakpoints(Boolean[] breakpoints) {
        this.breakpoints = breakpoints;
    }

    public DebuggerConfigsDTO getDebuggerConfigs() {
        return debuggerConfigs;
    }

    public void setDebuggerConfigs(DebuggerConfigsDTO debuggerConfigs) {
        this.debuggerConfigs = debuggerConfigs;
    }

    @Override
    public String toString() {
        return "DebuggerStateDTO{" +
                "currentRunningBT='" + currentRunningBT + '\'' +
                ", currentLineNumber=" + currentLineNumber +
                ", bThreadInfoList=" + bThreadInfoList +
                ", eventsStatus=" + eventsStatus +
                ", globalEnv=" + globalEnv +
                ", eventsHistory=" + eventsHistory +
                ", breakpoints=" + breakpoints +
                ", debuggerConfigs=" + debuggerConfigs +
                '}';
    }
}
