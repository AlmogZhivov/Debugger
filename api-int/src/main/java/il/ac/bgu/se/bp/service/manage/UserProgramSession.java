package il.ac.bgu.se.bp.service.manage;

import java.time.LocalDateTime;
import java.util.Objects;

public class UserProgramSession<T> extends UserSession {

    private T program;
    private String filename;

    public UserProgramSession(String sessionId, String userId, LocalDateTime lastOperationTime, String threadId) {
        super(sessionId, userId, lastOperationTime);
        setThreadId(threadId);
    }

    UserProgramSession<T> withFilename(String filename) {
        this.filename = filename;
        return this;
    }

    public T getProgram() {
        return program;
    }

    public void setProgram(T program) {
        this.program = program;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        UserProgramSession<?> that = (UserProgramSession<?>) o;
        return Objects.equals(program, that.program) &&
                Objects.equals(filename, that.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), program, filename);
    }

    @Override
    public String toString() {
        return "UserProgramSession{" +
                "program=" + program +
                ", filename='" + filename + '\'' +
                '}';
    }
}
