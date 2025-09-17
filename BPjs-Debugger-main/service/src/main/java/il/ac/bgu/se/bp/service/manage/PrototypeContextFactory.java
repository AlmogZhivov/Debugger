package il.ac.bgu.se.bp.service.manage;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class PrototypeContextFactory extends ContextFactory {

    private final static ConcurrentMap<String, Context> contextByThreadId = new ConcurrentHashMap<>();

    public PrototypeContextFactory() {
    }

    public Context enterContext() {
        final String threadId = Thread.currentThread().getName();
        contextByThreadId.putIfAbsent(threadId, Context.enter());
        return this.enterContext(contextByThreadId.get(threadId));
    }

    public void removeThread(String threadId) {
        contextByThreadId.remove(threadId);
    }
}
