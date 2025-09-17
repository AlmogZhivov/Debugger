package il.ac.bgu.se.bp.service.manage;


import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;

import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PrototypeContextFactoryTest {

    private PrototypeContextFactory prototypeContextFactory;
    private ConcurrentLinkedQueue<Context> contexts;

    @Before
    public void setUp() {
        prototypeContextFactory = new PrototypeContextFactory();
        contexts = new ConcurrentLinkedQueue<>();
    }

    @Test
    public void testContextFactory_uniqueContext() throws InterruptedException {
        int numOfThreads = 3;
        Thread[] threads = new Thread[numOfThreads];
        for (int i = 0; i < numOfThreads; i++) {
            threads[i] = new Thread(() -> contexts.add(prototypeContextFactory.enterContext()));
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(numOfThreads, contexts.size());
        do {
            Context currentContext = contexts.poll();
            for (Context context : contexts) {
                assertNotEquals(currentContext, context);
            }
        } while (!contexts.isEmpty());
    }
}
