package il.ac.bgu.se.bp.execution.manage;

import il.ac.bgu.se.bp.debugger.BPJsDebugger;
import il.ac.bgu.se.bp.debugger.DebuggerLevel;
import il.ac.bgu.se.bp.debugger.manage.DebuggerFactory;
import il.ac.bgu.se.bp.execution.COBPDebuggerImpl;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.utils.logger.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
/**
 * Factory implementation for creating COBP (Context-Oriented Behavior Programming) debuggers.
 * This factory creates COBPDebuggerImpl instances instead of the standard BPJsDebuggerImpl.
 */
public class COBPDebuggerFactoryImpl implements DebuggerFactory<BooleanResponse>, ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private static final Logger logger = new Logger(COBPDebuggerFactoryImpl.class);

    @Override
    public BPJsDebugger<BooleanResponse> getBPJsDebugger(String debuggerId, String filename, DebuggerLevel debuggerLevel) {
        logger.info("generating new COBP debugger for debuggerId: {0}, with filename: {1}", debuggerId, filename);
        BPJsDebugger<BooleanResponse> cobpDebugger = new COBPDebuggerImpl(debuggerId, filename, debuggerLevel);

        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(cobpDebugger);
        factory.initializeBean(cobpDebugger, cobpDebugger.getClass().getSimpleName());

        return cobpDebugger;
    }

    @Override
    public void setApplicationContext(@org.springframework.lang.NonNull ApplicationContext context) throws BeansException {
        applicationContext = context;
    }
}
