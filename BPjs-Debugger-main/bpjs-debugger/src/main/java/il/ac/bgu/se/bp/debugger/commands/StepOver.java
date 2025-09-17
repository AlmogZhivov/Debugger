package il.ac.bgu.se.bp.debugger.commands;

import il.ac.bgu.se.bp.debugger.engine.DebuggerEngine;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;

import static il.ac.bgu.se.bp.utils.ResponseHelper.createSuccessResponse;

public class StepOver implements DebuggerCommand {

    @Override
    public BooleanResponse applyCommand(DebuggerEngine debugger) {
        debugger.stepOver();
        return createSuccessResponse();
    }

    @Override
    public String toString() {
        return "StepOver";
    }
}