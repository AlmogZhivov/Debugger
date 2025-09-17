package il.ac.bgu.se.bp.debugger.manage;

import il.ac.bgu.se.bp.debugger.BPJsDebugger;
import il.ac.bgu.se.bp.debugger.DebuggerLevel;

@FunctionalInterface
public interface DebuggerFactory<T> {
    BPJsDebugger<T> getBPJsDebugger(String debuggerId, String filename, DebuggerLevel debuggerLevel);
}
