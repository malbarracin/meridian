package com.meridian.dashboard.dashboard.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meridian.dashboard.dashboard.dto.SummaryResponse;
import com.meridian.dashboard.dashboard.dto.NewsResponse;
import com.meridian.dashboard.dashboard.service.NewsService;
import com.meridian.dashboard.dashboard.service.DashboardService;
import com.meridian.dashboard.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Expone el resumen de dólar ARS, criptomonedas y bolsa protegido por JWT.
 *
 * <p>Las secciones se resuelven de forma independiente: si un proveedor falla, el endpoint responde
 * igualmente {@code 200} y lista la sección afectada en {@code degraded}.</p>
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Tag(name = "Dashboard", description = "Datos públicos agregados de mercado y noticias")
public class DashboardController {

    private final DashboardService dashboardService;
    private final NewsService newsService;

    /**
     * @param topic tópico IA/tecnología, por defecto ai-tech
     * @param limit máximo de artículos entre uno y ocho
     * @return noticias disponibles con indicador de degradación
     */
    @GetMapping("/news")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Obtener noticias IA/tecnología",
            description = "El fallback HN es normal. degraded solo indica el fallo de ambas fuentes.")
    @ApiResponse(responseCode = "200", description = "Noticias disponibles, frescas o stale",
            content = @Content(schema = @Schema(implementation = NewsResponse.class),
                    examples = @ExampleObject(ref = "#/components/examples/NewsMock")))
    @ApiResponse(responseCode = "400", description = "Tópico o límite inválido",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public NewsResponse getNews(
            @Parameter(description = "Único tópico soportado", schema = @Schema(allowableValues = "ai-tech"))
            @RequestParam(defaultValue = "ai-tech") String topic,
            @Parameter(description = "Máximo de artículos")
            @RequestParam(defaultValue = "8") @Min(1) @Max(8) Integer limit) {
        return newsService.getNews(topic, limit);
    }

    /**
     * Devuelve las tres secciones del resumen en una sola respuesta.
     *
     * @return {@code 200} con los datos disponibles y la lista de secciones sin datos frescos;
     *         {@code 500} con el contrato de error si falla la orquestación completa
     */
    @GetMapping("/summary")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class)))
    @Operation(summary = "Obtener el dashboard agregado",
            description = "Resumen de dólar ARS, criptomonedas y bolsa. Los fallos se informan en degraded.")
    @ApiResponse(responseCode = "200", description = "Datos agregados del dashboard",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = SummaryResponse.class),
                    examples = @ExampleObject(name = "modoMock", ref = "#/components/examples/SummaryMock")))
    @ApiResponse(responseCode = "500", description = "Error interno inesperado durante la orquestación",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "errorInterno", ref = "#/components/examples/InternalError")))
    public SummaryResponse getSummary() {
        return dashboardService.getSummary();
    }
}
