package il.ac.bgu.se.bp.config;

import il.ac.bgu.se.bp.session.ITSessionManager;
import il.ac.bgu.se.bp.session.ITSessionManagerImpl;
import il.ac.bgu.se.bp.testService.ControllerTestHelper;
import il.ac.bgu.se.bp.testService.TestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile({"BDT", "default"})
public class IDEBeforeDeploymentConfiguration {

    private ITSessionManagerImpl sessionHandlerMock = new ITSessionManagerImpl();

    @Bean
    TestService testService() {
        return new ControllerTestHelper();
    }

    @Bean
    ITSessionManagerImpl sessionHandler() {
        return sessionHandlerMock;
    }

    @Bean
    ITSessionManager itSessionManager() {
        return sessionHandlerMock;
    }
}
