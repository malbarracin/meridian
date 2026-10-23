package com.meridian.dashboard;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"DASHBOARD_USE_MOCK=true", "PORT=8081"})
@DisplayName("Arranque del dashboard sin proveedores externos")
class DashboardApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Test
    @DisplayName("Carga el contexto servlet con seis clientes y modo mock")
    void loadsContextInMockMode() {
        Map<String, WebClient> clients = context.getBeansOfType(WebClient.class);
        assertThat(clients).hasSize(6);
        assertThat(clients.keySet()).containsExactlyInAnyOrder(
                "dolarApiWebClient",
                "frankfurterWebClient",
                "coinGeckoWebClient",
                "finnhubWebClient",
                "newsApiWebClient",
                "hnAlgoliaWebClient");
        assertThat(environment.getProperty("dashboard.use-mock", Boolean.class)).isTrue();
        assertThat(environment.getProperty("server.port", Integer.class)).isEqualTo(8081);
        assertThat(environment.getProperty("dashboard.cache.ttl-seconds", Integer.class)).isEqualTo(60);
        assertThat(environment.getProperty("dashboard.providers.finnhub.api-key")).isEmpty();
        assertThat(environment.getProperty("dashboard.providers.news-api.api-key")).isEmpty();
    }
}
