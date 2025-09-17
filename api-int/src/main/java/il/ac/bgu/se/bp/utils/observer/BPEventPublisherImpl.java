package il.ac.bgu.se.bp.utils.observer;

import java.util.LinkedList;
import java.util.List;

public class BPEventPublisherImpl implements Publisher<BPEvent> {

    private List<Subscriber<BPEvent>> subscribers = new LinkedList<>();

    @Override
    public void subscribe(Subscriber<BPEvent> subscriber) {
        this.subscribers.add(subscriber);
    }

    @Override
    public void unsubscribe(Subscriber<BPEvent> subscriber) {
        this.subscribers.remove(subscriber);
    }

    @Override
    public void notifySubscribers(BPEvent bpEvent) {
        for (Subscriber<BPEvent> subscriber : subscribers) {
            subscriber.update(bpEvent);
        }
    }
}
