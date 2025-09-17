package il.ac.bgu.se.bp.utils.observer;

import il.ac.bgu.se.bp.utils.visitor.PublisherVisitor;

import java.util.Objects;

public abstract class BPEvent<T> {

    protected String debuggerId;
    protected T event;

    public BPEvent(String debuggerId, T event) {
        this.debuggerId = debuggerId;
        this.event = event;
    }

    abstract public void accept(PublisherVisitor visitor);
    abstract public String getEventType();

    public String getDebuggerId() {
        return debuggerId;
    }

    public void setDebuggerId(String debuggerId) {
        this.debuggerId = debuggerId;
    }

    public T getEvent() {
        return event;
    }

    public void setEvent(T event) {
        this.event = event;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BPEvent<?> bpEvent = (BPEvent<?>) o;
        return Objects.equals(debuggerId, bpEvent.debuggerId) &&
                Objects.equals(event, bpEvent.event);
    }

    @Override
    public int hashCode() {
        return Objects.hash(debuggerId, event);
    }

    @Override
    public String toString() {
        return "BPEvent{" +
                "debuggerId='" + debuggerId + '\'' +
                ", event=" + event +
                '}';
    }
}
