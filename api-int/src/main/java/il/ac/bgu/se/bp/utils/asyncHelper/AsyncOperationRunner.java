package il.ac.bgu.se.bp.utils.asyncHelper;

import java.util.concurrent.Callable;

@FunctionalInterface
public interface AsyncOperationRunner {
    void runAsyncCallback(Callable callback);
}
