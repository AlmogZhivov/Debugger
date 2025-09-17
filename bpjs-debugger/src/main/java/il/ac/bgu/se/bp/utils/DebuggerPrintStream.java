package il.ac.bgu.se.bp.utils;

import il.ac.bgu.se.bp.debugger.engine.events.BPConsoleEvent;
import il.ac.bgu.se.bp.socket.console.ConsoleMessage;
import il.ac.bgu.se.bp.socket.console.LogType;
import il.ac.bgu.se.bp.utils.observer.BPEvent;
import il.ac.bgu.se.bp.utils.observer.Publisher;
import il.ac.bgu.se.bp.utils.observer.Subscriber;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class DebuggerPrintStream extends PrintStream implements Publisher<BPEvent> {
    private List<Subscriber<BPEvent>> subscribers;
    private String debuggerId;

    public DebuggerPrintStream(OutputStream outputStream) {
        super(outputStream);
        subscribers = new ArrayList<>();
    }

    public DebuggerPrintStream() {
        this(System.out);
    }

    public void setDebuggerId(String debuggerId) {
        this.debuggerId = debuggerId;
    }

    @Override
    public void subscribe(Subscriber<BPEvent> subscriber) {
        subscribers.add(subscriber);
    }

    @Override
    public void unsubscribe(Subscriber<BPEvent> subscriber) {
        this.subscribers.remove(subscriber);
    }

    @Override
    public void notifySubscribers(BPEvent event) {
        for (Subscriber<BPEvent> subscriber : subscribers) {
            subscriber.update(event);
        }
    }
    @Override
    public void println(String s) {
        notifySubscribers(new BPConsoleEvent(debuggerId, new ConsoleMessage(s, LogType.log)));
    }
}
