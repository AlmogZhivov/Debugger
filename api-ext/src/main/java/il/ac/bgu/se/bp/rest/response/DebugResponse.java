package il.ac.bgu.se.bp.rest.response;

import il.ac.bgu.se.bp.error.ErrorCode;

import java.util.Arrays;

public class DebugResponse extends BooleanResponse {

    private boolean[] breakpoints;

    public DebugResponse() {
    }

    public DebugResponse(BooleanResponse booleanResponse) {
        super(booleanResponse.isSuccess(), booleanResponse.getErrorCode());
    }

    public DebugResponse(boolean isSuccess, boolean[] breakpoints) {
        super(isSuccess);
        this.breakpoints = breakpoints;
    }

    public DebugResponse(boolean isSuccess, ErrorCode errorCode, boolean[] breakpoints) {
        super(isSuccess, errorCode);
        this.breakpoints = breakpoints;
    }

    public boolean[] getBreakpoints() {
        return breakpoints;
    }

    public void setBreakpoints(boolean[] breakpoints) {
        this.breakpoints = breakpoints;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", breakpoints" + Arrays.toString(breakpoints) +
                '}';
    }
}
