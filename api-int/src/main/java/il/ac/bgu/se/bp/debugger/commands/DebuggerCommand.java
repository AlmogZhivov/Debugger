package il.ac.bgu.se.bp.debugger.commands;

import il.ac.bgu.se.bp.debugger.engine.DebuggerEngine;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;

@FunctionalInterface
public interface DebuggerCommand {
    BooleanResponse applyCommand(DebuggerEngine debugger);
}