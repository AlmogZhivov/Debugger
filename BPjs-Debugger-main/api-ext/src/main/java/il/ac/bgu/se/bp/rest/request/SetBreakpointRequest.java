package il.ac.bgu.se.bp.rest.request;

import java.io.Serializable;
import java.util.Objects;

public class SetBreakpointRequest implements Serializable {
    private static final long serialVersionUID = 7494433596960237849L;

    private int lineNumber;
    private boolean stopOnBreakpoint;

    public SetBreakpointRequest() {
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public boolean isStopOnBreakpoint() {
        return stopOnBreakpoint;
    }

    public void setStopOnBreakpoint(boolean stopOnBreakpoint) {
        this.stopOnBreakpoint = stopOnBreakpoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SetBreakpointRequest that = (SetBreakpointRequest) o;
        return lineNumber == that.lineNumber &&
                stopOnBreakpoint == that.stopOnBreakpoint;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumber, stopOnBreakpoint);
    }

    @Override
    public String toString() {
        return "SetBreakpointRequest{" +
                "lineNumber=" + lineNumber +
                ", stopOnBreakpoint=" + stopOnBreakpoint +
                '}';
    }
}
