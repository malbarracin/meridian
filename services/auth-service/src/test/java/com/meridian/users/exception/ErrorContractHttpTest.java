package com.meridian.users.exception;

import java.time.Instant;

import com.fasterxml.jackson.databind.SerializationFeature;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ErrorContractHttpTest {

    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ErrorController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(
                    Jackson2ObjectMapperBuilder.json()
                            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                            .build()))
            .build();

    @ParameterizedTest
    @CsvSource({"bad-request,400,BAD_REQUEST", "missing,404,NOT_FOUND", "conflict,409,CONFLICT",
            "duplicate,409,CONFLICT", "unexpected,500,INTERNAL_ERROR"})
    @DisplayName("El advice traduce excepciones a JSON sin detalles internos")
    void devuelveContratoPorHttp(String scenario, int expectedStatus, String expectedCode) throws Exception {
        Instant before = Instant.now();

        var response = mockMvc.perform(get("/errors/{scenario}", scenario))
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value(expectedCode))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(content().string(not(containsString("secret"))))
                .andExpect(content().string(not(containsString("Exception"))))
                .andReturn();

        var json = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(response.getResponse().getContentAsString());
        assertThat(Instant.parse(json.get("timestamp").asText())).isBetween(before, Instant.now());
    }

    @Test
    @DisplayName("La validación HTTP identifica el campo rechazado")
    void devuelveCampoInvalido() throws Exception {
        mockMvc.perform(post("/errors").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[0].message").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }

    @Test
    @DisplayName("ErrorCode expone los códigos del contrato")
    void exponeCodigosDelContrato() {
        assertThat(ErrorCode.values()).extracting(Enum::name)
                .containsExactly("VALIDATION_ERROR", "BAD_REQUEST", "NOT_FOUND", "CONFLICT", "UNAUTHORIZED",
                        "INTERNAL_ERROR");
    }

    @RestController
    static class ErrorController {

        @GetMapping("/errors/{scenario}")
        public void fail(@PathVariable String scenario) {
            switch (scenario) {
                case "bad-request" -> throw new IllegalArgumentException("secret bad input");
                case "missing" -> throw new NotFoundException("Recurso inexistente") { };
                case "conflict" -> throw new ConflictException("Conflicto de estado") { };
                case "duplicate" -> throw new DuplicateKeyException("secret index document");
                default -> throw new IllegalStateException("secret internal failure");
            }
        }

        @PostMapping("/errors")
        public void validate(@Valid @RequestBody ValidatedRequest request) {
        }
    }

    record ValidatedRequest(@NotBlank String name) {
    }
}
