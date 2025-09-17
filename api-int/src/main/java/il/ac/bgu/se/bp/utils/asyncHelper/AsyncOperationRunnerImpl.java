package il.ac.bgu.se.bp.utils.asyncHelper;

import il.ac.bgu.se.bp.utils.logger.Logger;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@EnableAsync
@Component
public class AsyncOperationRunnerImpl implements AsyncOperationRunner {

    private final static Logger logger = new Logger(AsyncOperationRunnerImpl.class);

    @Async
    @Override
    public void runAsyncCallback(Callable callback) {
        try {
            callback.call();
        } catch (Exception e) {
            logger.error("failed running async callback", e);
        }
    }

}
