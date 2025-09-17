package il.ac.bgu.se.bp.debugger;

public class RunnerState {
    public enum State {
        INITIALIZE,
        STOPPED,
        RUNNING,
        SYNC_STATE,
        WAITING_FOR_EXTERNAL_EVENT,
        JS_DEBUG
    }
    private State debuggerState;

    public RunnerState() {
        this.debuggerState = State.INITIALIZE;
    }

    public synchronized State getDebuggerState() {
        return debuggerState;
    }

    public synchronized void setDebuggerState(State debuggerState) {
        this.debuggerState = debuggerState;
    }
}
