package il.ac.bgu.se.bp.rest.request;

import java.io.Serializable;
import java.util.Objects;

public class ToggleSyncStatesRequest implements Serializable {

    private static final long serialVersionUID = 4589320075797164423L;

    private boolean skipSyncStates;

    public ToggleSyncStatesRequest() {
    }

    public ToggleSyncStatesRequest(boolean skipSyncStates) {
        this.skipSyncStates = skipSyncStates;
    }

    public boolean isSkipSyncStates() {
        return skipSyncStates;
    }

    public void setSkipSyncStates(boolean skipSyncStates) {
        this.skipSyncStates = skipSyncStates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ToggleSyncStatesRequest that = (ToggleSyncStatesRequest) o;
        return skipSyncStates == that.skipSyncStates;
    }

    @Override
    public int hashCode() {
        return Objects.hash(skipSyncStates);
    }

    @Override
    public String toString() {
        return "ToggleSyncStatesRequest{" +
                "skipSyncStates=" + skipSyncStates +
                '}';
    }
}
