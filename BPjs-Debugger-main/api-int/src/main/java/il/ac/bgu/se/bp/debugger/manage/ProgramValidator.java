package il.ac.bgu.se.bp.debugger.manage;

import il.ac.bgu.se.bp.debugger.RunnerState;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;

import java.util.concurrent.Callable;

public interface ProgramValidator<T> {
    BooleanResponse validateNextSync(T bProg);
    BooleanResponse validateJSDebugState(T bProg);
    BooleanResponse validateNotJSDebugState(T bProg);

    BooleanResponse validateAndRun(T bProg, Callable<BooleanResponse> callback);
    BooleanResponse validateAndRun(T bProg, RunnerState.State expectedState, Callable<BooleanResponse> callback);

    BooleanResponse validateAndRunAsync(T bProg, Callable<BooleanResponse> callback);
    BooleanResponse validateAndRunAsync(T bProg, RunnerState.State expectedState, Callable<BooleanResponse> callback);
}
