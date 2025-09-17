package il.ac.bgu.se.bp.debugger.engine.events;

import il.ac.bgu.se.bp.socket.console.ConsoleMessage;
import il.ac.bgu.se.bp.utils.observer.BPEvent;
import il.ac.bgu.se.bp.utils.visitor.PublisherVisitor;

public class BPConsoleEvent extends BPEvent<ConsoleMessage> {

    public BPConsoleEvent(String debuggerId, ConsoleMessage event) {
        super(debuggerId, event);
    }

    @Override
    public void accept(PublisherVisitor visitor) {
        visitor.visit(debuggerId, event);
    }

    @Override
    public String getEventType() {
        return "ConsoleMessage";
    }

}
