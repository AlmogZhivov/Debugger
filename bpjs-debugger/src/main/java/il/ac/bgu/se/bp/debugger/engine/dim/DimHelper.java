package il.ac.bgu.se.bp.debugger.engine.dim;

import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.tools.debugger.Dim;
import org.mozilla.javascript.tools.debugger.GuiCallback;

public interface DimHelper {

    void setGuiCallback(GuiCallback callback);
    void attachTo(ContextFactory factory);
    void stop();
    void setReturnValue(int returnValue);
    void go();
    boolean isBreakpointAllowed(int lineNumber, String filename);
    void setBreakpoint(int lineNumber, boolean stopOnBreakpoint, String filename);
    Dim.SourceInfo getSourceInfo(String filename);
}
