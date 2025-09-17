package il.ac.bgu.se.bp.debugger.engine;

import il.ac.bgu.se.bp.debugger.DebuggerLevel;
import il.ac.bgu.se.bp.debugger.commands.DebuggerCommand;
import il.ac.bgu.se.bp.utils.observer.BPEvent;
import il.ac.bgu.se.bp.utils.observer.Publisher;
import org.mozilla.javascript.tools.debugger.GuiCallback;

import java.util.Map;

public interface DebuggerEngine<S> extends GuiCallback, Publisher<BPEvent> {
    boolean isBreakpointAllowed(int lineNumber);
    void setBreakpoint(int lineNumber, boolean stopOnBreakpoint) throws IllegalArgumentException;
    void stop();
    void stepOut();
    void stepInto();
    void stepOver();
    void continueRun();
    void toggleMuteBreakpoints(boolean toggleBreakPointStatus);
    void getState();

    void setSyncSnapshot(S syncSnapshot);
    void setupBreakpoints(Map<Integer, Boolean> breakpoints);
    void addCommand(DebuggerCommand command) throws Exception;

    void onStateChanged();
    void changeDebuggerLevel(DebuggerLevel debuggerLevel);

    boolean[] getBreakpoints();

    boolean isMuteBreakpoints();
    boolean isRunning();
}
