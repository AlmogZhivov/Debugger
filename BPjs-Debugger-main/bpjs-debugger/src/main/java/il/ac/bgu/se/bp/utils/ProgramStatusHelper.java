package il.ac.bgu.se.bp.utils;

import il.ac.bgu.se.bp.debugger.DebuggerLevel;
import il.ac.bgu.se.bp.socket.status.Status;

public class ProgramStatusHelper {

    public static Status getRunStatusByDebuggerLevel(DebuggerLevel debuggerLevel) {
        if (DebuggerLevel.NORMAL.equals(debuggerLevel)) {
            return Status.DEBUG;
        }
        else if (DebuggerLevel.LIGHT.equals(debuggerLevel)) {
            return Status.RUN;
        }
        return null;
    }
}
