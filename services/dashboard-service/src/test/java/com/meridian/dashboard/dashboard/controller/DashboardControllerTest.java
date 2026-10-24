package com.meridian.dashboard.dashboard.controller;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.meridian.dashboard.dashboard.dto.DashboardResponse;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;
import com.meridian.dashboard.dashboard.service.DashboardService;

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

    @Test
    @DisplayName("Devuelve 200 con todas las secciones y degraded vacío")
    void devuelveDashboard() throws Exception {
        when(service.getDashboard()).thenReturn(new DashboardResponse(
                DashboardMockFixtures.getDolar(), DashboardMockFixtures.getFx(),
                DashboardMockFixtures.getCrypto(), DashboardMockFixtures.getCryptoHistory(),
                DashboardMockFixtures.getEquities(), DashboardMockFixtures.getEquityCandles(),
                DashboardMockFixtures.getNews(), List.of()));

        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dolar[0].tipo").value("oficial"))
                .andExpect(jsonPath("$.fx").isNotEmpty())
                .andExpect(jsonPath("$.crypto").isNotEmpty())
                .andExpect(jsonPath("$.cryptoHistory").isNotEmpty())
                .andExpect(jsonPath("$.equities").isNotEmpty())
                .andExpect(jsonPath("$.equityCandles").isNotEmpty())
                .andExpect(jsonPath("$.news").isNotEmpty())
                .andExpect(jsonPath("$.degraded").isEmpty());
    }

    @Test
    @DisplayName("Devuelve 200 con secciones vacías y degraded parcial")
    void devuelveDegradedParcial() throws Exception {
        when(service.getDashboard()).thenReturn(new DashboardResponse(
                List.of(), DashboardMockFixtures.getFx(), DashboardMockFixtures.getCrypto(),
                List.of(), List.of(), List.of(), List.of(), List.of("dolar", "news")));

        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", bearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dolar").isEmpty())
                .andExpect(jsonPath("$.crypto").isNotEmpty())
                .andExpect(jsonPath("$.degraded[0]").value("dolar"))
                .andExpect(jsonPath("$.degraded[1]").value("news"));
    }

    @Test
    @DisplayName("El handler oculta errores inesperados de orquestación")
    void manejaErrorInesperado() throws Exception {
        when(service.getDashboard()).thenThrow(new IllegalStateException("secret"));
        mockMvc.perform(get("/api/v1/dashboard").header("Authorization", bearerToken()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
