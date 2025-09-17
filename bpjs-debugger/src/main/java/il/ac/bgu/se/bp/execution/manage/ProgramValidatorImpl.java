package il.ac.bgu.se.bp.execution.manage;

import il.ac.bgu.se.bp.debugger.BPJsDebugger;
import il.ac.bgu.se.bp.debugger.RunnerState;
import il.ac.bgu.se.bp.debugger.manage.ProgramValidator;
import il.ac.bgu.se.bp.error.ErrorCode;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.utils.asyncHelper.AsyncOperationRunner;
import il.ac.bgu.se.bp.utils.logger.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

import static il.ac.bgu.se.bp.utils.ResponseHelper.createErrorResponse;
import static il.ac.bgu.se.bp.utils.ResponseHelper.createSuccessResponse;

@Component
public class ProgramValidatorImpl implements ProgramValidator<BPJsDebugger> {

    private final static Logger logger = new Logger(ProgramValidatorImpl.class);

    @Autowired
    private AsyncOperationRunner asyncOperationRunner;

    @Override
    public BooleanResponse validateNextSync(BPJsDebugger bProg) {
        RunnerState state = bProg.getDebuggerState();

        if (state.getDebuggerState() == RunnerState.State.WAITING_FOR_EXTERNAL_EVENT) {
            return createErrorResponse(ErrorCode.WAITING_FOR_EXTERNAL_EVENT);
        }
        else if (state.getDebuggerState() == RunnerState.State.JS_DEBUG) {
            return createErrorResponse(ErrorCode.NOT_IN_BP_SYNC_STATE);
        }
        else if (state.getDebuggerState() == RunnerState.State.RUNNING) {
            return createErrorResponse(ErrorCode.ALREADY_RUNNING);
        }
        else if (!bProg.isStarted()) {
            return createErrorResponse(ErrorCode.NOT_STARTED);
        }

        return createSuccessResponse();
    }

    @Override
    public BooleanResponse validateJSDebugState(BPJsDebugger bProg) {
        RunnerState state = bProg.getDebuggerState();

        if (state.getDebuggerState() != RunnerState.State.JS_DEBUG) {
            return createErrorResponse(ErrorCode.NOT_IN_JS_DEBUG_STATE);
        }

        return createSuccessResponse();
    }

    @Override
    public BooleanResponse validateNotJSDebugState(BPJsDebugger bProg) {
        return !validateJSDebugState(bProg).isSuccess() ? createSuccessResponse() :
                createErrorResponse(ErrorCode.CANNOT_ADD_EXTERNAL_EVENT_ON_JS_DEBUG_STATE);
    }

    @Override
    public BooleanResponse validateAndRun(BPJsDebugger bProg, Callable<BooleanResponse> callback) {
        if (!bProg.isSetup()) {
            return createErrorResponse(ErrorCode.SETUP_REQUIRED);
        }

        try {
            return callback.call();
        } catch (Exception e) {
            logger.error("failed running callback after validation", e);
            e.printStackTrace();
        }

        return createErrorResponse(ErrorCode.GENERAL_ERROR);
    }

    @Override
    public BooleanResponse validateAndRun(BPJsDebugger bProg, RunnerState.State expectedState, Callable<BooleanResponse> callback) {
        if (!bProg.isSetup()) {
            return createErrorResponse(ErrorCode.SETUP_REQUIRED);
        }

        RunnerState.State state = bProg.getDebuggerState().getDebuggerState();

        if (!RunnerState.State.JS_DEBUG.equals(state)) {
            return createErrorResponse(getErrorCodeByExpectedDebuggerState(expectedState));
        }

        return validateAndRun(bProg, callback);
    }

    @Override
    public BooleanResponse validateAndRunAsync(BPJsDebugger bProg, Callable<BooleanResponse> callback) {
        if (!bProg.isSetup()) {
            return createErrorResponse(ErrorCode.SETUP_REQUIRED);
        }

        asyncOperationRunner.runAsyncCallback(callback);
        return createSuccessResponse();
    }

    @Override
    public BooleanResponse validateAndRunAsync(BPJsDebugger bProg, RunnerState.State expectedState, Callable<BooleanResponse> callback) {
        if (!bProg.isSetup()) {
            return createErrorResponse(ErrorCode.SETUP_REQUIRED);
        }
        RunnerState.State actualState = bProg.getDebuggerState().getDebuggerState();

        if (!expectedState.equals(actualState)) {
            return createErrorResponse(getErrorCodeByExpectedDebuggerState(expectedState));
        }

        return validateAndRunAsync(bProg, callback);
    }

    private ErrorCode getErrorCodeByExpectedDebuggerState(RunnerState.State state) {
        switch (state) {
            case JS_DEBUG:
                return ErrorCode.NOT_IN_JS_DEBUG_STATE;
            case INITIALIZE:
            case RUNNING:
                return ErrorCode.NOT_RUNNING;
            case SYNC_STATE:
                return ErrorCode.NOT_IN_BP_SYNC_STATE;
            case STOPPED:
                return ErrorCode.ALREADY_RUNNING;
            case WAITING_FOR_EXTERNAL_EVENT:
                return ErrorCode.NOT_WAITING_FOR_EXTERNAL_EVENT;
        }

        return ErrorCode.GENERAL_ERROR;
    }
}
