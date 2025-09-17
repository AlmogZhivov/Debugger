package il.ac.bgu.se.bp.rest.response;

import il.ac.bgu.se.bp.socket.state.BPDebuggerState;

import java.util.Objects;
import java.util.SortedMap;

public class GetSyncSnapshotsResponse {

    private SortedMap<Long, BPDebuggerState> syncSnapShotsHistory;

    public GetSyncSnapshotsResponse() {
    }

    public GetSyncSnapshotsResponse(SortedMap<Long, BPDebuggerState> syncSnapShotsHistory) {
        this.syncSnapShotsHistory = syncSnapShotsHistory;
    }

    public SortedMap<Long, BPDebuggerState> getSyncSnapShotsHistory() {
        return syncSnapShotsHistory;
    }

    public void setSyncSnapShotsHistory(SortedMap<Long, BPDebuggerState> syncSnapShotsHistory) {
        this.syncSnapShotsHistory = syncSnapShotsHistory;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetSyncSnapshotsResponse that = (GetSyncSnapshotsResponse) o;
        return Objects.equals(syncSnapShotsHistory, that.syncSnapShotsHistory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(syncSnapShotsHistory);
    }

    @Override
    public String toString() {
        return "GetSyncSnapshotsResponse{" +
                "syncSnapShotsHistory=" + syncSnapShotsHistory.toString() +
                '}';
    }
}
