package com.meridian.dashboard.dashboard.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.reactive.function.client.WebClient;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "DASHBOARD_USE_MOCK=true")
@AutoConfigureMockMvc
@DisplayName("Dashboard offline completo")
class DashboardMockIntegrationTest {

    private static String bearerToken() {
        return "Bearer " + com.meridian.dashboard.security.JwtValidatorTest.token(
                com.meridian.dashboard.security.JwtValidatorTest.SECRET,
                java.time.Instant.now().plusSeconds(300));
    }

    @Autowired private MockMvc mockMvc;
    @MockitoBean(name = "dolarApiWebClient") private WebClient dolarClient;
    @MockitoBean(name = "frankfurterWebClient") private WebClient fxClient;
    @MockitoBean(name = "coinGeckoWebClient") private WebClient cryptoClient;
    @MockitoBean(name = "finnhubWebClient") private WebClient equityClient;
    @MockitoBean(name = "newsApiWebClient") private WebClient newsClient;
    @MockitoBean(name = "hnAlgoliaWebClient") private WebClient hnClient;

    @Test
    @DisplayName("Devuelve fixtures de todas las secciones sin utilizar clientes HTTP")
    void devuelveFixturesSinHttp() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dolar").isNotEmpty())
                .andExpect(jsonPath("$.fx").isNotEmpty())
                .andExpect(jsonPath("$.crypto").isNotEmpty())
                .andExpect(jsonPath("$.cryptoHistory.length()").value(16))
                .andExpect(jsonPath("$.equities").isNotEmpty())
                .andExpect(jsonPath("$.equityCandles").isNotEmpty())
                .andExpect(jsonPath("$.news").isNotEmpty())
                .andExpect(jsonPath("$.degraded").isEmpty());
        verifyNoInteractions(dolarClient, fxClient, cryptoClient, equityClient, newsClient, hnClient);
    }
}
