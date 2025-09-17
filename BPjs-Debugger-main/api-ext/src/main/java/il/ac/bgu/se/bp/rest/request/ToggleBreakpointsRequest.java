package il.ac.bgu.se.bp.rest.request;

import java.io.Serializable;
import java.util.Objects;

public class ToggleBreakpointsRequest implements Serializable {

    private static final long serialVersionUID = 1752871358409839423L;

    private boolean skipBreakpoints;

    public ToggleBreakpointsRequest() {
    }

    public ToggleBreakpointsRequest(boolean skipBreakpoints) {
        this.skipBreakpoints = skipBreakpoints;
    }

    public boolean isSkipBreakpoints() {
        return skipBreakpoints;
    }

    public void setSkipBreakpoints(boolean skipBreakpoints) {
        this.skipBreakpoints = skipBreakpoints;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ToggleBreakpointsRequest that = (ToggleBreakpointsRequest) o;
        return skipBreakpoints == that.skipBreakpoints;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipBreakpoints);
    }

    @Override
    public String toString() {
        return "ToggleBreakpointsRequest{" +
                "skipBreakpoints=" + skipBreakpoints +
                '}';
    }
}
