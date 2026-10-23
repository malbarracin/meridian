package com.meridian.dashboard.config;

import java.time.Duration;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/** Clientes por proveedor con límites de conexión y respuesta, sin llamadas al arrancar. */
@Configuration(proxyBeanMethods = false)
public class WebClientConfig {

    @Bean
    WebClient dolarApiWebClient(WebClient.Builder builder, Environment environment) {
        return createClient(builder, environment, "dolar-api");
    }

    @Bean
    WebClient frankfurterWebClient(WebClient.Builder builder, Environment environment) {
        return createClient(builder, environment, "frankfurter");
    }

    @Bean
    WebClient coinGeckoWebClient(WebClient.Builder builder, Environment environment) {
        return createClient(builder, environment, "coin-gecko");
    }

    @Bean
    WebClient finnhubWebClient(WebClient.Builder builder, Environment environment) {
        return createClient(builder, environment, "finnhub");
    }

    @Bean
    WebClient newsApiWebClient(WebClient.Builder builder, Environment environment) {
        return createClient(builder, environment, "news-api");
    }

    @Bean
    WebClient hnAlgoliaWebClient(WebClient.Builder builder, Environment environment) {
        return createClient(builder, environment, "hn-algolia");
    }

    private WebClient createClient(WebClient.Builder builder, Environment environment, String provider) {
        String prefix = "dashboard.providers." + provider;
        long seconds = environment.getRequiredProperty(prefix + ".timeout-seconds", Long.class);
        if (seconds <= 0) {
            throw new IllegalArgumentException("El timeout del proveedor debe ser positivo");
        }
        Duration timeout = Duration.ofSeconds(seconds);
        HttpClient client = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(timeout.toMillis()))
                .responseTimeout(timeout);
        return builder.clone()
                .baseUrl(environment.getRequiredProperty(prefix + ".base-url"))
                .clientConnector(new ReactorClientHttpConnector(client))
                .build();
    }
}
