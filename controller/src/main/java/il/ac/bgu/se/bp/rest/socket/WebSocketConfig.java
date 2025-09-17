package il.ac.bgu.se.bp.rest.socket;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import static il.ac.bgu.se.bp.rest.utils.Endpoints.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker(STATE, CONSOLE, PROGRAM);
        config.setApplicationDestinationPrefixes(BASE_URI);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint(BASE_WEB_SOCKET_URI)
                .setAllowedOrigins("*")
                .setHandshakeHandler(new CustomHandshakeHandler());

        registry
                .addEndpoint(BASE_WEB_SOCKET_URI)
                .setAllowedOrigins("*")
                .setHandshakeHandler(new CustomHandshakeHandler())
                .withSockJS();
    }


}

