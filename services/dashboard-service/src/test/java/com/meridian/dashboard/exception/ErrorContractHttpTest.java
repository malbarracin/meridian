package com.meridian.dashboard.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Contrato HTTP uniforme de errores")
class ErrorContractHttpTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ErrorController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("Oculta el mensaje interno de errores inesperados")
    void hidesUnexpectedError() throws Exception {
        mockMvc.perform(get("/error/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Error interno"))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Traduce ausencia y conflicto de dominio")
    void mapsDomainErrors() throws Exception {
        mockMvc.perform(get("/error/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        mockMvc.perform(get("/error/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    @DisplayName("Rechaza parámetros ausentes, tipos inválidos y argumentos inválidos")
    void rejectsBadRequests() throws Exception {
        mockMvc.perform(get("/error/parameter"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mockMvc.perform(get("/error/parameter").param("number", "abc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mockMvc.perform(get("/error/argument"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        mockMvc.perform(post("/error/body").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"));
    }

    @Test
    @DisplayName("Devuelve detalles por campo al validar el cuerpo")
    void reportsBodyValidation() throws Exception {
        mockMvc.perform(post("/error/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }

    @Test
    @DisplayName("Devuelve detalles de restricciones en parámetros")
    void reportsConstraintValidation() throws Exception {
        mockMvc.perform(get("/error/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("number"));
    }

    record Body(@NotBlank String name) {
    }

    record Parameters(@Min(1) int number) {
    }

    @RestController
    static class ErrorController {

        @GetMapping("/error/unexpected")
        String unexpected() {
            throw new IllegalStateException("secret-provider-token");
        }

        @GetMapping("/error/missing")
        String missing() {
            throw new NotFoundException("No existe el recurso") { };
        }

        @GetMapping("/error/conflict")
        String conflict() {
            throw new ConflictException("El recurso ya existe") { };
        }

        @GetMapping("/error/argument")
        String argument() {
            throw new IllegalArgumentException("secret");
        }

        @GetMapping("/error/parameter")
        int parameter(@RequestParam int number) {
            return number;
        }

        @PostMapping("/error/body")
        String body(@Valid @RequestBody Body body) {
            return body.name();
        }

        @GetMapping("/error/constraint")
        String constraint() {
            try (var validator = new LocalValidatorFactoryBean()) {
                validator.afterPropertiesSet();
                throw new jakarta.validation.ConstraintViolationException(validator.validate(new Parameters(0)));
            }
        }
    }
}
