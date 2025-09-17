package il.ac.bgu.se.bp.config;

import il.ac.bgu.se.bp.debugger.manage.DebuggerFactory;
import il.ac.bgu.se.bp.debugger.manage.ProgramValidator;
import il.ac.bgu.se.bp.execution.manage.DebuggerFactoryImpl;
import il.ac.bgu.se.bp.execution.manage.ProgramValidatorImpl;
import il.ac.bgu.se.bp.mains.BPJsDebuggerCliRunner;
import il.ac.bgu.se.bp.utils.asyncHelper.AsyncOperationRunner;
import il.ac.bgu.se.bp.utils.asyncHelper.AsyncOperationRunnerImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class BPJsDebuggerConfiguration {

    @Bean
    public BPJsDebuggerCliRunner bpJsDebuggerCliRunner() {
        return new BPJsDebuggerCliRunner();
    }

    @Bean
    public DebuggerFactory debuggerFactory() {
        return new DebuggerFactoryImpl();
    }

    @Bean
    public AsyncOperationRunner asyncOperationRunner() {
        return new AsyncOperationRunnerImpl();
    }

    @Bean
    public ProgramValidator programValidator() {
        return new ProgramValidatorImpl();
    }
}
