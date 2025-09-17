package il.ac.bgu.se.bp.debugger.engine.events;

import il.ac.bgu.se.bp.socket.state.BPDebuggerState;
import il.ac.bgu.se.bp.utils.observer.BPEvent;
import il.ac.bgu.se.bp.utils.visitor.PublisherVisitor;

public class BPStateEvent extends BPEvent<BPDebuggerState> {

    public BPStateEvent(String debuggerId, BPDebuggerState event) {
        super(debuggerId, event);
    }

    @Override
    public void accept(PublisherVisitor visitor) {
        visitor.visit(debuggerId, event);
    }

    @Override
    public String getEventType() {
        return "BPDebuggerState";
    }
}
