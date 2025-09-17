package il.ac.bgu.se.bp.rest.request;

import java.io.Serializable;
import java.util.Objects;

public class RunRequest implements Serializable {
    private static final long serialVersionUID = 4758855571059683558L;

    protected String sourceCode;
    protected boolean waitForExternalEvents;

    public RunRequest() {
    }

    public RunRequest(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public boolean isWaitForExternalEvents() {
        return waitForExternalEvents;
    }

    public void setWaitForExternalEvents(boolean waitForExternalEvents) {
        this.waitForExternalEvents = waitForExternalEvents;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RunRequest that = (RunRequest) o;
        return waitForExternalEvents == that.waitForExternalEvents &&
                Objects.equals(sourceCode, that.sourceCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceCode, waitForExternalEvents);
    }

    @Override
    public String toString() {
        return "RunRequest{" +
                "sourceCode='" + sourceCode + '\'' +
                ", waitForExternalEvents=" + waitForExternalEvents +
                '}';
    }
}
