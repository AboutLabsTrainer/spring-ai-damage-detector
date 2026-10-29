package de.aboutlabs.damagedetector;

import de.aboutlabs.damagedetector.domain.Jev;
import de.aboutlabs.damagedetector.domain.PassPolicy;
import de.aboutlabs.damagedetector.jev.OpenRouterJev;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DetectorProperties.class)
class DetectorConfiguration {

    @Bean
    PassPolicy passPolicy(DetectorProperties properties) {
        return new PassPolicy(properties.passThreshold());
    }

    @Bean
    Jev jev(RestClient.Builder restClientBuilder, DetectorProperties properties) {
        return new OpenRouterJev(restClientBuilder, properties.jev());
    }
}
