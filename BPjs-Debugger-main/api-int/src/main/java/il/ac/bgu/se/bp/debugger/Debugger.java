package il.ac.bgu.se.bp.debugger;

public interface Debugger<T> {
    T setBreakpoint(int lineNumber, boolean stopOnBreakpoint);
    T stop();
    T stepOut();
    T stepInto();
    T stepOver();
    T continueRun();
    T toggleMuteBreakpoints(boolean toggleBreakPointStatus);
    T getState();
}
