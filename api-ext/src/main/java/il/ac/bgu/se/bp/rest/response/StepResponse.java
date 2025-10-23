package il.ac.bgu.se.bp.rest.response;

/**
 * Response for step operations that includes both success status and debugger state.
 * This allows HTTP clients to get the debugger state directly without relying on WebSocket events.
 * Uses StepStateDTO to avoid circular reference issues during serialization.
 */
public class StepResponse {
    private boolean success;
    private String errorCode;
    private StepStateDTO debuggerState;

    public StepResponse() {
    }

    public StepResponse(boolean success, String errorCode, StepStateDTO debuggerState) {
        this.success = success;
        this.errorCode = errorCode;
        this.debuggerState = debuggerState;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public StepStateDTO getDebuggerState() {
        return debuggerState;
    }

    public void setDebuggerState(StepStateDTO debuggerState) {
        this.debuggerState = debuggerState;
    }

    @Override
    public String toString() {
        return "StepResponse{" +
                "success=" + success +
                ", errorCode='" + errorCode + '\'' +
                ", debuggerState=" + debuggerState +
                '}';
    }
}
