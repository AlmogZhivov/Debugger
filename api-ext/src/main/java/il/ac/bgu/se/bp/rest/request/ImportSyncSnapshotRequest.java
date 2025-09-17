package il.ac.bgu.se.bp.rest.request;

import il.ac.bgu.se.bp.rest.response.SyncSnapshot;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class ImportSyncSnapshotRequest implements Serializable {
    private static final long serialVersionUID = 7556994057798147933L;

    private SyncSnapshot syncSnapshot;
    private boolean debug;
    private List<Integer> breakpoints = new LinkedList<>();
    private boolean skipBreakpointsToggle;
    private boolean skipSyncStateToggle;
    private boolean waitForExternalEvents;

    public ImportSyncSnapshotRequest() {
    }

    public SyncSnapshot getSyncSnapshot() {
        return syncSnapshot;
    }

    public void setSyncSnapshot(SyncSnapshot syncSnapshot) {
        this.syncSnapshot = syncSnapshot;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public List<Integer> getBreakpoints() {
        return breakpoints;
    }

    public void setBreakpoints(List<Integer> breakpoints) {
        this.breakpoints = breakpoints;
    }

    public boolean isSkipBreakpointsToggle() {
        return skipBreakpointsToggle;
    }

    public void setSkipBreakpointsToggle(boolean skipBreakpointsToggle) {
        this.skipBreakpointsToggle = skipBreakpointsToggle;
    }

    public boolean isSkipSyncStateToggle() {
        return skipSyncStateToggle;
    }

    public void setSkipSyncStateToggle(boolean skipSyncStateToggle) {
        this.skipSyncStateToggle = skipSyncStateToggle;
    }

    public boolean isWaitForExternalEvents() {
        return waitForExternalEvents;
    }

    public void setWaitForExternalEvents(boolean waitForExternalEvents) {
        this.waitForExternalEvents = waitForExternalEvents;
    }

    @Override
    public boolean equals(Object queryType) {
        if (this == queryType) {
            return true;
        }
        if (queryType == null || getClass() != queryType.getClass()) {
            return false;
        }
        ImportSyncSnapshotRequest that = (ImportSyncSnapshotRequest) queryType;
        return debug == that.debug &&
                skipBreakpointsToggle == that.skipBreakpointsToggle &&
                skipSyncStateToggle == that.skipSyncStateToggle &&
                waitForExternalEvents == that.waitForExternalEvents &&
                Objects.equals(syncSnapshot, that.syncSnapshot) &&
                Objects.equals(breakpoints, that.breakpoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(syncSnapshot, debug, breakpoints, skipBreakpointsToggle, skipSyncStateToggle, waitForExternalEvents);
    }

    @Override
    public String toString() {
        return "ImportSyncSnapshotRequest{" +
                "syncSnapshot=" + syncSnapshot +
                ", isDebug=" + debug +
                ", breakpoints=" + breakpoints +
                ", skipBreakpointsToggle=" + skipBreakpointsToggle +
                ", skipSyncStateToggle=" + skipSyncStateToggle +
                ", waitForExternalEvents=" + waitForExternalEvents +
                '}';
    }
}
