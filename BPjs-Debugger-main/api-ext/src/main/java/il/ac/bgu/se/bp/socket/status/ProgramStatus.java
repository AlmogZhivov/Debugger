package il.ac.bgu.se.bp.socket.status;

import java.io.Serializable;
import java.util.Objects;

public class ProgramStatus implements Serializable {
    private static final long serialVersionUID = -1206523140785645037L;

    private Status status;

    public ProgramStatus() {
    }

    public ProgramStatus(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProgramStatus that = (ProgramStatus) o;
        return status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(status);
    }

    @Override
    public String toString() {
        return "ProgramStatus{" +
                "status=" + status +
                '}';
    }
}
