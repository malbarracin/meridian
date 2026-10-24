package com.meridian.dashboard.security;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meridian.dashboard.exception.UnsupportedParameterException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(JwtHttpIntegrationTest.ProbeController.class)
@ActiveProfiles("jwt-http-test")
@DisplayName("Alcance HTTP de autenticación y contrato de errores")
class JwtHttpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Protege raíz y subrutas del dashboard con error 401 uniforme")
    void protectsDashboard() throws Exception {
        for (String path : new String[]{"/api/v1/dashboard", "/api/v1/dashboard/security-probe"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                    .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
                    .andExpect(jsonPath("$.details").isEmpty())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());
        }
    }

    @Test
    @DisplayName("Token válido permite llegar al handler de parámetros no soportados")
    void mapsUnsupportedParameter() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/security-probe").header("Authorization", "Bearer "
                + JwtValidatorTest.token(JwtValidatorTest.SECRET, Instant.now().plusSeconds(300))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("La solicitud tiene datos inválidos"))
                .andExpect(jsonPath("$.details[0].field").value("asset"))
                .andExpect(jsonPath("$.details[0].message").value("valor no soportado"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Actuator y documentación continúan públicos")
    void keepsInfrastructurePublic() throws Exception {
        for (String path : new String[]{"/actuator/health", "/actuator/info", "/v3/api-docs",
                "/swagger-ui/index.html"}) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
    }

    @RestController
    @Profile("jwt-http-test")
    static class ProbeController {

        @GetMapping("/api/v1/dashboard/security-probe")
        String unsupported() {
            throw new UnsupportedParameterException("asset");
        }
    }
}
