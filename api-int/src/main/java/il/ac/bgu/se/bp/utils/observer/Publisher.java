package il.ac.bgu.se.bp.utils.observer;

public interface Publisher<T> {

    void subscribe(Subscriber<T> subscriber);
    void unsubscribe(Subscriber<T> subscriber);

    void notifySubscribers(T event);
}
