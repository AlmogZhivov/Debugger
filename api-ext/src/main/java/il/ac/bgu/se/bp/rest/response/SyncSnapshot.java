package il.ac.bgu.se.bp.rest.response;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

public class SyncSnapshot implements Serializable {
    private static final long serialVersionUID = 8433468883477207141L;

    private String sourceCode;
    private byte[] syncSnapshot;

    public SyncSnapshot() {
    }

    public SyncSnapshot(String sourceCode, byte[] syncSnapshot) {
        this.sourceCode = sourceCode;
        this.syncSnapshot = syncSnapshot;
    }

    public String getSourceCode() {
        return sourceCode;
    }

    public void setSourceCode(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public byte[] getSyncSnapshot() {
        return syncSnapshot;
    }

    public void setSyncSnapshot(byte[] syncSnapshot) {
        this.syncSnapshot = syncSnapshot;
    }

    @Override
    public boolean equals(Object queryType) {
        if (this == queryType) {
            return true;
        }
        if (queryType == null || getClass() != queryType.getClass()) {
            return false;
        }
        SyncSnapshot that = (SyncSnapshot) queryType;
        return Objects.equals(sourceCode, that.sourceCode) &&
                Arrays.equals(syncSnapshot, that.syncSnapshot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceCode, syncSnapshot);
    }

    @Override
    public String toString() {
        return "SyncSnapshot{" +
                "sourceCode='" + sourceCode + '\'' +
                ", syncSnapshot=" + Arrays.toString(syncSnapshot) +
                '}';
    }
}
