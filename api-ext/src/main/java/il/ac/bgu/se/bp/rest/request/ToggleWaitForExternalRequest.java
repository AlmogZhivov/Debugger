package il.ac.bgu.se.bp.rest.request;

import java.io.Serializable;
import java.util.Objects;

public class ToggleWaitForExternalRequest implements Serializable {

    private static final long serialVersionUID = 1752871358409839423L;

    private boolean waitForExternal;

    public ToggleWaitForExternalRequest() {
    }

    public ToggleWaitForExternalRequest(boolean waitForExternal) {
        this.waitForExternal = waitForExternal;
    }

    public boolean isWaitForExternal() {
        return waitForExternal;
    }

    public void setWaitForExternal(boolean waitForExternal) {
        this.waitForExternal = waitForExternal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ToggleWaitForExternalRequest that = (ToggleWaitForExternalRequest) o;
        return waitForExternal == that.waitForExternal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(waitForExternal);
    }

    @Override
    public String toString() {
        return "ToggleBreakpointsRequest{" +
                "waitForExternal=" + waitForExternal +
                '}';
    }
}
