package com.open.spring.mvc.groups;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class that sets up a dedicated secondary port
 * for WebSocket traffic on the embedded Tomcat server if a distinct socket port 
 * is specified in the application configuration alongside the main server port.
 */
@Configuration
public class ChatWebSocketPortConfig {

    @Value("${server.port:8585}")
    private int serverPort;

    @Value("${socket.port:8589}")
    private int socketPort;

    @Bean
    public TomcatServletWebServerFactory tomcatServletWebServerFactory() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        if (socketPort != serverPort) {
            factory.addAdditionalTomcatConnectors(createConnector(socketPort));
        }
        return factory;
    }

    private Connector createConnector(int port) {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setPort(port);
        return connector;
    }
}
