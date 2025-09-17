package il.ac.bgu.se.bp.utils.observer;

public interface Subscriber<T> {
    void update(T event);
}
