package il.ac.bgu.se.bp.socket.state;

import java.io.Serializable;
import java.util.Objects;

public class DebuggerConfigs implements Serializable {
    private static final long serialVersionUID = 983874658293202361L;

    private boolean toggleMuteBreakPoint;
    private boolean toggleWaitForExternalEvents;
    private boolean toggleMuteSyncPoints;

    public DebuggerConfigs() {
    }

    public DebuggerConfigs(boolean toggleMuteBreakPoint, boolean toggleWaitForExternalEvents, boolean toggleMuteSyncPoints) {
        this.toggleMuteBreakPoint = toggleMuteBreakPoint;
        this.toggleWaitForExternalEvents = toggleWaitForExternalEvents;
        this.toggleMuteSyncPoints = toggleMuteSyncPoints;
    }

    public boolean isToggleMuteBreakPoint() {
        return toggleMuteBreakPoint;
    }

    public void setToggleMuteBreakPoint(boolean toggleMuteBreakPoint) {
        this.toggleMuteBreakPoint = toggleMuteBreakPoint;
    }

    public boolean isToggleWaitForExternalEvents() {
        return toggleWaitForExternalEvents;
    }

    public void setToggleWaitForExternalEvents(boolean toggleWaitForExternalEvents) {
        this.toggleWaitForExternalEvents = toggleWaitForExternalEvents;
    }

    public boolean isToggleMuteSyncPoints() {
        return toggleMuteSyncPoints;
    }

    public void setToggleMuteSyncPoints(boolean toggleMuteSyncPoints) {
        this.toggleMuteSyncPoints = toggleMuteSyncPoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DebuggerConfigs debuggerConfigs = (DebuggerConfigs) o;
        return toggleMuteBreakPoint == debuggerConfigs.toggleMuteBreakPoint &&
                toggleMuteSyncPoints == debuggerConfigs.toggleMuteSyncPoints &&
                toggleWaitForExternalEvents == debuggerConfigs.toggleWaitForExternalEvents;
    }

    @Override
    public int hashCode() {
        return Objects.hash(toggleMuteBreakPoint, toggleMuteSyncPoints, toggleWaitForExternalEvents);
    }

    @Override
    public String toString() {
        return "DebuggerConfigs{" +
                "toggleMuteBreakPoint=" + toggleMuteBreakPoint +
                ", toggleWaitForExternalEvents=" + toggleWaitForExternalEvents +
                ", toggleMuteSyncPoints=" + toggleMuteSyncPoints +
                '}';
    }
}
