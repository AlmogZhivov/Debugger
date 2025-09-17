package il.ac.bgu.se.bp.rest.request;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class DebugRequest extends RunRequest {
    private static final long serialVersionUID = 6588719020597509990L;

    private List<Integer> breakpoints = new LinkedList<>();
    private boolean skipBreakpointsToggle;
    private boolean skipSyncStateToggle;

    public DebugRequest() {
    }

    public DebugRequest(String code, List<Integer> breakpoints) {
        this.sourceCode = code;
        this.breakpoints = breakpoints;
    }


    public DebugRequest(String code) {
        this.sourceCode = code;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DebugRequest that = (DebugRequest) o;
        return skipBreakpointsToggle == that.skipBreakpointsToggle && skipSyncStateToggle == that.skipSyncStateToggle && Objects.equals(breakpoints, that.breakpoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), breakpoints, skipBreakpointsToggle, skipSyncStateToggle);
    }

    @Override
    public String toString() {
        return "DebugRequest{" +
                "breakpoints=" + breakpoints +
                ", skipBreakpointsToggle=" + skipBreakpointsToggle +
                ", skipSyncStateToggle=" + skipSyncStateToggle +
                ", sourceCode='" + sourceCode + '\'' +
                '}';
    }
}
