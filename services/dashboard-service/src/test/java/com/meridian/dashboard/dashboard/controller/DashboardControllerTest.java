package com.meridian.dashboard.dashboard.controller;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meridian.dashboard.dashboard.dto.FxSection;
import com.meridian.dashboard.dashboard.dto.NewsResponse;
import com.meridian.dashboard.dashboard.dto.SummaryResponse;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;
import com.meridian.dashboard.dashboard.service.DashboardService;
import com.meridian.dashboard.dashboard.service.NewsService;
import com.meridian.dashboard.exception.UnsupportedParameterException;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@DisplayName("Endpoint del dashboard")
class DashboardControllerTest {

    private static String bearerToken() {
        return "Bearer " + com.meridian.dashboard.security.JwtValidatorTest.token(
                com.meridian.dashboard.security.JwtValidatorTest.SECRET,
                java.time.Instant.now().plusSeconds(300));
    }

    @Autowired private MockMvc mockMvc;
    @MockitoBean private DashboardService service;
    @MockitoBean private NewsService newsService;

    @Test
    @DisplayName("Noticias devuelve los campos normalizados con JWT")
    void devuelveNoticias() throws Exception {
        when(newsService.getNews("ai-tech", 8)).thenReturn(
                new NewsResponse(DashboardMockFixtures.getNews(), false));
        mockMvc.perform(get("/api/v1/dashboard/news").header("Authorization", bearerToken())
                        .param("topic", "ai-tech").param("limit", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").isNotEmpty())
                .andExpect(jsonPath("$.items[0].title").isNotEmpty())
                .andExpect(jsonPath("$.items[0].summary").isNotEmpty())
                .andExpect(jsonPath("$.items[0].source").isNotEmpty())
                .andExpect(jsonPath("$.items[0].publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.items[0].url").isNotEmpty())
                .andExpect(jsonPath("$.items[0].topic").value("ai-tech"))
                .andExpect(jsonPath("$.degraded").value(false));
        mockMvc.perform(get("/api/v1/dashboard/news").header("Authorization", bearerToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Noticias exige JWT y rechaza parámetros inválidos con contrato estándar")
    void rechazaNoticiasInvalidas() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/news"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/dashboard/news").header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
        when(newsService.getNews("sports", 8)).thenThrow(
                new UnsupportedParameterException("topic"));
        mockMvc.perform(get("/api/v1/dashboard/news").header("Authorization", bearerToken())
                        .param("topic", "sports"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("topic"));
        for (String limit : List.of("0", "9", "-1")) {
            mockMvc.perform(get("/api/v1/dashboard/news").header("Authorization", bearerToken())
                            .param("limit", limit))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.details[0].field").value("limit"))
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    @Test
    @DisplayName("Devuelve 200 con todas las secciones y degraded vacío")
    void devuelveDashboard() throws Exception {
        when(service.getSummary()).thenReturn(new SummaryResponse(
                "Meridian", java.time.Instant.now(), DashboardMockFixtures.getSummaryFx(),
                DashboardMockFixtures.getSummaryCrypto(), DashboardMockFixtures.getSummaryEquities(), List.of()));

        mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("Meridian"))
                .andExpect(jsonPath("$.generatedAt").isNotEmpty())
                .andExpect(jsonPath("$.fx.quotes.usd_oficial.compra").value(1000))
                .andExpect(jsonPath("$.fx.quotes.usd_oficial.venta").value(1040))
                .andExpect(jsonPath("$.fx.quotes.usd_blue.compra").value(1200))
                .andExpect(jsonPath("$.fx.quotes.usd_blue.venta").value(1220))
                .andExpect(jsonPath("$.crypto.bitcoin.priceUsd").value(67000))
                .andExpect(jsonPath("$.crypto.bitcoin.change24hPct").value(2.5))
                .andExpect(jsonPath("$.crypto.ethereum.priceUsd").value(3500))
                .andExpect(jsonPath("$.crypto.ethereum.change24hPct").value(-1.2))
                .andExpect(jsonPath("$.equities.AAPL.price").value(175))
                .andExpect(jsonPath("$.equities.AAPL.currency").value("USD"))
                .andExpect(jsonPath("$.equities.MSFT.price").value(415))
                .andExpect(jsonPath("$.equities.MSFT.currency").value("USD"))
                .andExpect(jsonPath("$.equities.SPY.price").value(450))
                .andExpect(jsonPath("$.equities.SPY.currency").value("USD"))
                .andExpect(jsonPath("$.activeUsers").doesNotExist())
                .andExpect(jsonPath("$.sessionsToday").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist())
                .andExpect(jsonPath("$.degraded").isEmpty());
    }

    @Test
    @DisplayName("Devuelve 200 con secciones vacías y degraded parcial")
    void devuelveDegradedParcial() throws Exception {
        when(service.getSummary()).thenReturn(new SummaryResponse(
                "Meridian", java.time.Instant.now(), new FxSection(Map.of()),
                DashboardMockFixtures.getSummaryCrypto(), DashboardMockFixtures.getSummaryEquities(), List.of("fx")));

        mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fx.quotes").isEmpty())
                .andExpect(jsonPath("$.crypto").isNotEmpty())
                .andExpect(jsonPath("$.degraded[0]").value("fx"))
                .andExpect(jsonPath("$.degraded.length()").value(1));
    }

    @Test
    @DisplayName("Rechaza JWT ausente, inválido o expirado sin datos de mercado")
    void rechazaCredenciales() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.fx").doesNotExist())
                .andExpect(jsonPath("$.crypto").doesNotExist())
                .andExpect(jsonPath("$.equities").doesNotExist());
        for (String authorization : List.of("", "Bearer invalid", "Bearer "
                + com.meridian.dashboard.security.JwtValidatorTest.token(
                        com.meridian.dashboard.security.JwtValidatorTest.SECRET,
                        java.time.Instant.now().minusSeconds(60)))) {
            mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", authorization))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.fx").doesNotExist())
                    .andExpect(jsonPath("$.crypto").doesNotExist())
                    .andExpect(jsonPath("$.equities").doesNotExist());
        }
        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    @DisplayName("Retira la ruta anterior")
    void retiraRutaAnterior() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", bearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        org.mockito.Mockito.verifyNoInteractions(service);
    }

    @Test
    @DisplayName("El handler oculta errores inesperados de orquestación")
    void manejaErrorInesperado() throws Exception {
        when(service.getSummary()).thenThrow(new IllegalStateException("secret"));
        mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", bearerToken()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
