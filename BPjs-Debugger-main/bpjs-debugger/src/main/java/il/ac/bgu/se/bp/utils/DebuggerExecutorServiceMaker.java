package il.ac.bgu.se.bp.utils;

import il.ac.bgu.cs.bp.bpjs.internal.ExecutorServiceMaker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class DebuggerExecutorServiceMaker extends ExecutorServiceMaker {

    public ExecutorService makeWithName(String threadNameTemplate ) {
        final ThreadFactory dtf = Executors.defaultThreadFactory();
        final AtomicInteger threadCoutner = new AtomicInteger(0);
        ThreadFactory tf = (Runnable r) -> {
            Thread retVal = dtf.newThread(r);
            retVal.setName(threadNameTemplate + "#" + threadCoutner.incrementAndGet() );
            return retVal;
        };

        return Executors.newFixedThreadPool(1, tf);
    }

}
