package il.ac.bgu.se.bp.rest.response;

/**
 * Serialization-safe DTO for debugger configurations.
 */
public class DebuggerConfigsDTO {
    private boolean isSkipBreakpoints;
    private boolean isSkipSyncPoints;
    private boolean isWaitForExternalEvents;

    public DebuggerConfigsDTO() {
    }

    public boolean isSkipBreakpoints() {
        return isSkipBreakpoints;
    }

    public void setSkipBreakpoints(boolean skipBreakpoints) {
        isSkipBreakpoints = skipBreakpoints;
    }

    public boolean isSkipSyncPoints() {
        return isSkipSyncPoints;
    }

    public void setSkipSyncPoints(boolean skipSyncPoints) {
        isSkipSyncPoints = skipSyncPoints;
    }

    public boolean isWaitForExternalEvents() {
        return isWaitForExternalEvents;
    }

    public void setWaitForExternalEvents(boolean waitForExternalEvents) {
        isWaitForExternalEvents = waitForExternalEvents;
    }

    @Override
    public String toString() {
        return "DebuggerConfigsDTO{" +
                "isSkipBreakpoints=" + isSkipBreakpoints +
                ", isSkipSyncPoints=" + isSkipSyncPoints +
                ", isWaitForExternalEvents=" + isWaitForExternalEvents +
                '}';
    }
}
