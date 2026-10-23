package com.meridian.dashboard.config;

import java.time.Duration;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Clientes HTTP de proveedores")
class WebClientConfigTest {

    @Test
    @DisplayName("Usa la URL configurada del proveedor")
    void usesConfiguredProviderUrl() throws Exception {
        try (var server = new MockWebServer()) {
            server.enqueue(new MockResponse().setBody("provider-response"));
            server.start();
            WebClient client = configuredClient(server);

            String response = client.get().uri("/quote").retrieve().bodyToMono(String.class)
                    .block(Duration.ofSeconds(5));

            assertThat(response).isEqualTo("provider-response");
            assertThat(server.takeRequest().getPath()).isEqualTo("/quote");
        }
    }

    @Test
    @DisplayName("Interrumpe una respuesta que excede el timeout")
    void boundsProviderResponseTime() throws Exception {
        try (var server = new MockWebServer()) {
            server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
            server.start();
            WebClient client = configuredClient(server);

            assertThatThrownBy(() -> client.get().retrieve().bodyToMono(String.class)
                    .block(Duration.ofSeconds(5)))
                    .isInstanceOf(WebClientRequestException.class)
                    .hasCauseInstanceOf(io.netty.handler.timeout.ReadTimeoutException.class);
        }
    }

    @Test
    @DisplayName("Rechaza timeouts no positivos al configurar el proveedor")
    void rejectsNonPositiveTimeout() {
        var environment = new MockEnvironment()
                .withProperty("dashboard.providers.dolar-api.timeout-seconds", "0");

        assertThatThrownBy(() -> new WebClientConfig().dolarApiWebClient(WebClient.builder(), environment))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private WebClient configuredClient(MockWebServer server) {
        var environment = new MockEnvironment()
                .withProperty("dashboard.providers.dolar-api.base-url", server.url("/").toString())
                .withProperty("dashboard.providers.dolar-api.timeout-seconds", "1");
        return new WebClientConfig().dolarApiWebClient(WebClient.builder(), environment);
    }
}
