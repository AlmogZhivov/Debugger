package il.ac.bgu.se.bp.socket.state;

import java.io.Serializable;
import java.util.*;

public class BPDebuggerState implements Serializable {
    private static final long serialVersionUID = 6320377753998745711L;

    private List<BThreadInfo> bThreadInfoList;
    private EventsStatus eventsStatus;
    private SortedMap<Long,EventInfo> eventsHistory;
    private String currentRunningBT;
    private Integer currentLineNumber;
    private DebuggerConfigs debuggerConfigs;
    private Boolean[] breakpoints;
    private Map<String, String> globalEnv;


    public BPDebuggerState() {
        this.bThreadInfoList = new ArrayList<>();
        this.eventsStatus = new EventsStatus(new ArrayList<>(), new ArrayList<>(), new HashSet<>() , new LinkedList<>());
        this.eventsHistory = new TreeMap<>();
        this.globalEnv = new LinkedHashMap<>();
    }

    public BPDebuggerState(List<BThreadInfo> bThreadInfoList, EventsStatus eventsStatus) {
        this.bThreadInfoList = bThreadInfoList;
        this.eventsStatus = eventsStatus;
        this.eventsHistory = new TreeMap<>();
        this.globalEnv = new LinkedHashMap<>();
    }

    public BPDebuggerState(List<BThreadInfo> bThreadInfoList, EventsStatus eventsStatus , SortedMap<Long,EventInfo> eventsHistory, String currentRunningBT, Integer currentLineNumber) {
        this.bThreadInfoList = bThreadInfoList;
        this.eventsStatus = eventsStatus;
        this.eventsHistory = eventsHistory;
        this.currentRunningBT = currentRunningBT;
        this.currentLineNumber = currentLineNumber;
        this.globalEnv = new LinkedHashMap<>();
    }

    public BPDebuggerState(List<BThreadInfo> bThreadInfoList, EventsStatus eventsStatus , SortedMap<Long,EventInfo> eventsHistory, String currentRunningBT, Integer currentLineNumber, DebuggerConfigs debuggerConfigs, Boolean[] breakpoints, Map<String, String> globalEnv) {
        this.bThreadInfoList = bThreadInfoList;
        this.eventsStatus = eventsStatus;
        this.eventsHistory = eventsHistory;
        this.currentRunningBT = currentRunningBT;
        this.currentLineNumber = currentLineNumber;
        this.debuggerConfigs = debuggerConfigs;
        this.breakpoints = breakpoints;
        this.globalEnv= globalEnv;
    }

    public Map<String, String> getGlobalEnv() {
        return globalEnv;
    }

    public void setGlobalEnv(Map<String, String> globalEnv) {
        this.globalEnv = globalEnv;
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

    public List<BThreadInfo> getbThreadInfoList() {
        return bThreadInfoList;
    }

    public void setbThreadInfoList(List<BThreadInfo> bThreadInfoList) {
        this.bThreadInfoList = bThreadInfoList;
    }

    public EventsStatus getEventsStatus() {
        return eventsStatus;
    }

    public void setEventsStatus(EventsStatus eventsStatus) {
        this.eventsStatus = eventsStatus;
    }

    public Boolean[] getBreakpoints() {
        return breakpoints;
    }

    public void setBreakpoints(Boolean[] breakpoints) {
        this.breakpoints = breakpoints;
    }

    public SortedMap<Long, EventInfo> getEventsHistory() {
        return eventsHistory;
    }

    public void setEventsHistory(SortedMap<Long, EventInfo> eventsHistory) {
        this.eventsHistory = eventsHistory;
    }

    public DebuggerConfigs getDebuggerConfigs() {
        return debuggerConfigs;
    }

    public void setDebuggerConfigs(DebuggerConfigs debuggerConfigs) {
        this.debuggerConfigs = debuggerConfigs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BPDebuggerState that = (BPDebuggerState) o;
        return bThreadInfoList.containsAll(that.bThreadInfoList) && that.bThreadInfoList.containsAll(bThreadInfoList) &&
                Objects.equals(eventsStatus, that.eventsStatus) &&
                Objects.equals(currentRunningBT, that.currentRunningBT) &&
                Objects.equals(currentLineNumber, that.currentLineNumber);
    }

    public String prettier() {
        StringBuilder s = new StringBuilder();
        s.append("\n{\n")
                .append("BPDebuggerState\n");

        bThreadInfoList.forEach(bThreadInfo -> s.append(bThreadInfo.prettier("\t")).append("\n\n"));

        s.append(eventsStatus.prettier("\t"))
                .append("\n")
                .append("\tcurrentRunningBT: ").append(currentRunningBT)
                .append("\n")
                .append("\tcurrentLineNumber: ").append(currentLineNumber)
                .append("\n")
                .append("\tdebuggerConfigs:\n").append(debuggerConfigs.toString())
                .append("\n")
                .append("globalEnv: ").append(globalEnv)
                .append("\t breakpoints:");
                s.append(Arrays.toString(breakpoints))
                        .append("\n")
                        .append("\teventsHistory: \n");
                eventsHistory.entrySet().forEach(eventInfo -> s.append("\t"+eventInfo.toString() + "\n"));
                s.append("}");

        return s.toString();
    }

    @Override
    public String toString() {
        return "BPDebuggerState{" +
                "bThreadInfoList=" + bThreadInfoList +
                ", eventsStatus=" + eventsStatus +
                ", eventsHistory=" + eventsHistory +
                ", currentRunningBT='" + currentRunningBT + '\'' +
                ", currentLineNumber=" + currentLineNumber +
                '}';
    }
}
