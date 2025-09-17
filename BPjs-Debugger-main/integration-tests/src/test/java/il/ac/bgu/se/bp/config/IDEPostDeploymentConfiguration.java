package il.ac.bgu.se.bp.config;

import il.ac.bgu.se.bp.session.ITSessionManagerImpl;
import il.ac.bgu.se.bp.testService.RestServiceTestHelper;
import il.ac.bgu.se.bp.testService.TestService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("PDT")
public class IDEPostDeploymentConfiguration {

    @Bean
    ITSessionManagerImpl sessionHandler() {
        return new ITSessionManagerImpl();
    }

    @Bean
    TestService serviceTestHelper(ITSessionManagerImpl sessionHandler) {
        return new RestServiceTestHelper(sessionHandler);
    }
}
