package com.meridian.dashboard.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
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
    @DisplayName("Responde 500 con ErrorResponse genérico ante Exception no manejada")
    void hidesUnexpectedError() throws Exception {
        mockMvc.perform(get("/error/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Error interno"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.message", not(containsString("secret-provider-token"))));
    }

    @Test
    @DisplayName("Traduce ausencia de dominio a 404 con cuerpo completo")
    void mapsNotFound() throws Exception {
        expectErrorBody(mockMvc.perform(get("/error/missing")), 404, "NOT_FOUND", "No existe el recurso");
    }

    @Test
    @DisplayName("Traduce conflicto de dominio a 409 con cuerpo completo")
    void mapsConflict() throws Exception {
        expectErrorBody(mockMvc.perform(get("/error/conflict")), 409, "CONFLICT", "El recurso ya existe");
    }

    @Test
    @DisplayName("Traduce credenciales inválidas a 401 UNAUTHORIZED")
    void mapsUnauthorized() throws Exception {
        expectErrorBody(mockMvc.perform(get("/error/unauthorized")), 401, "UNAUTHORIZED",
                "Credenciales inválidas");
    }

    @Test
    @DisplayName("Traduce parámetro no soportado a 400 VALIDATION_ERROR con detalle de campo")
    void mapsUnsupportedParameter() throws Exception {
        mockMvc.perform(get("/error/unsupported"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("La solicitud tiene datos inválidos"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0].field").value("asset"))
                .andExpect(jsonPath("$.details[0].message").value("valor no soportado"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Rechaza parámetros ausentes, tipos inválidos y argumentos inválidos")
    void rejectsBadRequests() throws Exception {
        expectBadRequest(mockMvc.perform(get("/error/parameter")));
        expectBadRequest(mockMvc.perform(get("/error/parameter").param("number", "abc")));
        expectBadRequest(mockMvc.perform(get("/error/argument")));
        expectBadRequest(mockMvc.perform(post("/error/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{")));
    }

    @Test
    @DisplayName("Devuelve detalles por campo al validar el cuerpo")
    void reportsBodyValidation() throws Exception {
        mockMvc.perform(post("/error/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("La solicitud tiene datos inválidos"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    @DisplayName("Devuelve detalles de restricciones en parámetros")
    void reportsConstraintValidation() throws Exception {
        mockMvc.perform(get("/error/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("La solicitud tiene datos inválidos"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0].field").value("number"))
                .andExpect(jsonPath("$.details[0].message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    private static void expectBadRequest(ResultActions actions) throws Exception {
        expectErrorBody(actions, 400, "BAD_REQUEST", "Solicitud inválida");
    }

    private static void expectErrorBody(ResultActions actions, int status, String code, String message)
            throws Exception {
        actions.andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
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
            throw new NotFoundException("No existe el recurso") {
            };
        }

        @GetMapping("/error/conflict")
        String conflict() {
            throw new ConflictException("El recurso ya existe") {
            };
        }

        @GetMapping("/error/unauthorized")
        String unauthorized() {
            throw new UnauthorizedException();
        }

        @GetMapping("/error/unsupported")
        String unsupported() {
            throw new UnsupportedParameterException("asset");
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
