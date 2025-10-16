package il.ac.bgu.se.bp.rest.response;

import il.ac.bgu.se.bp.error.ErrorCode;
import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import java.util.Objects;

public class BooleanResponse {

    private boolean isSuccess;
    private ErrorCode errorCode;
    private BPDebuggerState context;

    public BooleanResponse() { }

    public BooleanResponse(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public BooleanResponse(boolean isSuccess, ErrorCode errorCode) {
        this.isSuccess = isSuccess;
        this.errorCode = errorCode;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BooleanResponse that = (BooleanResponse) o;
        return isSuccess == that.isSuccess && errorCode == that.errorCode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isSuccess, errorCode);
    }

    public BPDebuggerState getContext() { 
        return context; 
    }
    
    public void setContext(BPDebuggerState context) { 
        this.context = context; 
    }

    @Override
    public String toString() {
        return "BooleanResponse{" +
                "isSuccess=" + isSuccess +
                ", errorCode=" + errorCode + '\'' +
                ", context=" + (context != null ? context.toString() : "null") +
                '}';
    }
}
