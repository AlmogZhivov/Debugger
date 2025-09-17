package il.ac.bgu.se.bp.debugger.commands;

import il.ac.bgu.se.bp.debugger.engine.DebuggerEngine;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;

import static il.ac.bgu.se.bp.utils.ResponseHelper.createSuccessResponse;

public class StepInto implements DebuggerCommand {

    @Override
    public BooleanResponse applyCommand(DebuggerEngine debugger) {
        debugger.stepInto();
        return createSuccessResponse();
    }

    @Override
    public String toString() {
        return "StepInto";
    }
}