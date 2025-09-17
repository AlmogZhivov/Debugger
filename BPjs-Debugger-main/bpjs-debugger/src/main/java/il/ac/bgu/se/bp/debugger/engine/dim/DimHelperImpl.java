package il.ac.bgu.se.bp.debugger.engine.dim;

import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.tools.debugger.Dim;
import org.mozilla.javascript.tools.debugger.GuiCallback;

public class DimHelperImpl implements DimHelper {

    private final Dim dim;

    public DimHelperImpl() {
        dim = new Dim();
    }

    @Override
    public void setGuiCallback(GuiCallback callback) {
        dim.setGuiCallback(callback);
    }

    @Override
    public void attachTo(ContextFactory factory) {
        dim.attachTo(factory);
    }

    @Override
    public void stop() {
        dim.setReturnValue(Dim.EXIT);
    }

    @Override
    public void setReturnValue(int returnValue) {
        dim.setReturnValue(returnValue);
    }

    @Override
    public void go() {
        dim.go();
    }

    @Override
    public Dim.SourceInfo getSourceInfo(String filename){
        return dim.sourceInfo(filename);
    }

    @Override
    public boolean isBreakpointAllowed(int lineNumber, String filename) {
        if (lineNumber < 0)
            return false;
        Dim.SourceInfo sourceInfo = dim.sourceInfo(filename);
        return sourceInfo.breakableLine(lineNumber);
    }

    @Override
    public void setBreakpoint(int lineNumber, boolean stopOnBreakpoint, String filename) {
        Dim.SourceInfo sourceInfo = dim.sourceInfo(filename);
        sourceInfo.breakpoint(lineNumber, stopOnBreakpoint);
    }




//    //todo: remove
//    public void getVars() {
//        StringBuilder vars = new StringBuilder();
//        Dim.ContextData currentContextData = dim.currentContextData();
//        for (int i = 0; i < currentContextData.frameCount(); i++) {
//            vars.append("Scope no: ").append(i).append("\n");
//            Dim.StackFrame stackFrame = currentContextData.getFrame(i);
//            NativeCall scope = (NativeCall) stackFrame.scope();
//            Object[] objects = ((Scriptable) scope).getIds();
//            List<String> arguments = Arrays.stream(objects).map(Object::toString).collect(Collectors.toList()).subList(1, objects.length);
//            for (String arg : arguments) {
//                Object res = ScriptableObject.getProperty(scope, arg);
//                if (Undefined.instance != res)
//                    vars.append(arg).append(" ").append(res).append("\n");
//            }
//        }
//        System.out.println("Vars: \n" + vars);
//    }

}
