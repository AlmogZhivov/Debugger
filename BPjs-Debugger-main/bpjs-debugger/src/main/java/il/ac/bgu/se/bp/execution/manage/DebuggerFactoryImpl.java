package il.ac.bgu.se.bp.execution.manage;

import il.ac.bgu.se.bp.debugger.BPJsDebugger;
import il.ac.bgu.se.bp.debugger.DebuggerLevel;
import il.ac.bgu.se.bp.debugger.manage.DebuggerFactory;
import il.ac.bgu.se.bp.execution.BPJsDebuggerImpl;
import il.ac.bgu.se.bp.rest.response.BooleanResponse;
import il.ac.bgu.se.bp.utils.logger.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class DebuggerFactoryImpl implements DebuggerFactory<BooleanResponse>, ApplicationContextAware {

    private static ApplicationContext applicationContext;
    private static final Logger logger = new Logger(DebuggerFactoryImpl.class);

    @Override
    public BPJsDebugger<BooleanResponse> getBPJsDebugger(String debuggerId, String filename, DebuggerLevel debuggerLevel) {
        logger.info("generating new debugger for debuggerId: {0}, with filename: {1}", debuggerId, filename);
        BPJsDebugger<BooleanResponse> bpJsDebugger = new BPJsDebuggerImpl(debuggerId, filename, debuggerLevel);

        AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
        factory.autowireBean(bpJsDebugger);
        factory.initializeBean(bpJsDebugger, bpJsDebugger.getClass().getSimpleName());

        return bpJsDebugger;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

}
