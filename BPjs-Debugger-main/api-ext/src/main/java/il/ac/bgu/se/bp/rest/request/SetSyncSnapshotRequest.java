package il.ac.bgu.se.bp.rest.request;

import java.io.Serializable;
import java.util.Objects;

public class SetSyncSnapshotRequest implements Serializable {
    private static final long serialVersionUID = -1140462904735197313L;

    private long snapShotTime;

    public SetSyncSnapshotRequest() {
    }

    public long getSnapShotTime() {
        return snapShotTime;
    }

    public void setSnapShotTime(long snapShotTime) {
        this.snapShotTime = snapShotTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SetSyncSnapshotRequest that = (SetSyncSnapshotRequest) o;
        return snapShotTime == that.snapShotTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(snapShotTime);
    }

    @Override
    public String toString() {
        return "SetSyncSnapshotRequest{" +
                "snapShotTime=" + snapShotTime +
                '}';
    }
}
