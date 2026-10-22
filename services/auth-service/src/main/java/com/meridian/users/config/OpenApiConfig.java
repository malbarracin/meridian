package com.meridian.users.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

/** Identidad y componentes reutilizables de la documentación pública del servicio. */
@Configuration
public class OpenApiConfig {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Define los metadatos, el servidor local y los ejemplos de error compartidos por todos
     * los endpoints.
     *
     * @return especificación base de la API de autenticación
     */
    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Service API")
                        .version("1.0")
                        .description("Registro y consulta de los datos públicos de usuarios con persistencia "
                                + "en MongoDB. El email se normaliza antes de guardarse y la contraseña solo "
                                + "se almacena como hash. Los errores usan el contrato uniforme "
                                + "code/message/details/timestamp."))
                .servers(List.of(new Server()
                        .url("http://localhost:8080")
                        .description("Entorno local")))
                .components(errorComponents());
    }

    private Components errorComponents() {
        return new Components()
                .addExamples("ValidationError", buildExample("Datos inválidos",
                        """
                        {
                          "code": "VALIDATION_ERROR",
                          "message": "La solicitud tiene datos inválidos",
                          "details": [
                            {
                              "field": "email",
                              "message": "must be a well-formed email address"
                            }
                          ],
                          "timestamp": "2026-10-22T10:05:00Z"
                        }"""))
                .addExamples("BadRequest", buildExample("Solicitud mal formada",
                        """
                        {
                          "code": "BAD_REQUEST",
                          "message": "Solicitud inválida",
                          "details": [],
                          "timestamp": "2026-10-22T10:05:00Z"
                        }"""))
                .addExamples("DuplicateEmail", buildExample("Email ya registrado",
                        """
                        {
                          "code": "CONFLICT",
                          "message": "Email ya registrado",
                          "details": [],
                          "timestamp": "2026-10-22T10:05:00Z"
                        }"""))
                .addExamples("NotFound", buildExample("Usuario inexistente",
                        """
                        {
                          "code": "NOT_FOUND",
                          "message": "Usuario no encontrado por id: 6710f3a2c9e77c00ffffffff",
                          "details": [],
                          "timestamp": "2026-10-22T10:05:00Z"
                        }"""))
                .addExamples("InternalError", buildExample("Error interno",
                        """
                        {
                          "code": "INTERNAL_ERROR",
                          "message": "Error interno",
                          "details": [],
                          "timestamp": "2026-10-22T10:05:00Z"
                        }"""));
    }

    private Example buildExample(String summary, String value) {
        try {
            JsonNode json = OBJECT_MAPPER.readTree(value);
            return new Example().summary(summary).value(json);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("El ejemplo OpenAPI no es JSON válido", exception);
        }
    }
}
