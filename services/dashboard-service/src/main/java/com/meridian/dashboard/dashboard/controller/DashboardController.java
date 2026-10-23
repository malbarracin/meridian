package com.meridian.dashboard.dashboard.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meridian.dashboard.dashboard.dto.DashboardResponse;
import com.meridian.dashboard.dashboard.service.DashboardService;
import com.meridian.dashboard.exception.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Expone el dashboard agregado con datos de dólar, FX, cripto, bolsa y noticias.
 *
 * <p>Las secciones se resuelven de forma independiente: si un proveedor falla, el endpoint responde
 * igualmente {@code 200} y lista la sección afectada en {@code degraded}.</p>
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Datos públicos agregados de mercado y noticias")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Devuelve todas las secciones del dashboard en una sola respuesta.
     *
     * @return {@code 200} con los datos disponibles y la lista de secciones sin datos frescos;
     *         {@code 500} con el contrato de error si falla la orquestación completa
     */
    @GetMapping
    @Operation(summary = "Obtener el dashboard agregado",
            description = "Agrega cotizaciones de dólar y FX, precios e histórico de cripto, quotes y velas "
                    + "de bolsa y noticias de IA/tecnología. Con DASHBOARD_USE_MOCK=true devuelve fixtures "
                    + "estáticas sin llamadas externas. Los fallos de un proveedor no generan error HTTP: "
                    + "la sección se informa en degraded y se sirve cache o una lista vacía.")
    @ApiResponse(responseCode = "200", description = "Datos agregados del dashboard",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = DashboardResponse.class),
                    examples = @ExampleObject(name = "modoMock", ref = "#/components/examples/DashboardMock")))
    @ApiResponse(responseCode = "500", description = "Error interno inesperado durante la orquestación",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(name = "errorInterno", ref = "#/components/examples/InternalError")))
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }
}
