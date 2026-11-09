package de.aboutlabs.damagedetector;

import de.aboutlabs.damagedetector.domain.Jev;
import de.aboutlabs.damagedetector.domain.PassPolicy;
import de.aboutlabs.damagedetector.jev.OpenRouterJev;
import de.aboutlabs.damagedetector.jev.RetryingJev;
import de.aboutlabs.damagedetector.jev.Sleeper;
import java.time.Clock;
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
    Jev jev(RestClient.Builder restClientBuilder, DetectorProperties properties, Sleeper sleeper) {
        DetectorProperties.Jev jev = properties.jev();
        DetectorProperties.Retry retry = jev.retry();
        OpenRouterJev adapter = new OpenRouterJev(restClientBuilder, jev.baseUrl(), jev.model(), jev.apiKey(),
            jev.connectTimeout(), jev.readTimeout());
        return RetryingJev.around(adapter, retry.maxAttempts(), retry.initialBackoff(), retry.maxBackoff(), sleeper);
    }

    @Bean
    Sleeper sleeper() {
        return Sleeper.realTime();
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
