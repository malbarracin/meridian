package com.meridian.dashboard.dashboard.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meridian.dashboard.dashboard.dto.ChartsResponse;
import com.meridian.dashboard.dashboard.dto.NewsResponse;
import com.meridian.dashboard.dashboard.dto.SummaryResponse;
import com.meridian.dashboard.dashboard.service.ChartsService;
import com.meridian.dashboard.dashboard.service.DashboardService;
import com.meridian.dashboard.dashboard.service.NewsService;
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
 * Endpoints del dashboard protegidos por JWT (emitidos por auth-service).
 * <p>Expone tres endpoints: resumen agregado, series históricas y noticias IA/tecnología.
 * Las secciones se resuelven de forma independiente: si un proveedor falla, se responde {@code 200}
 * con la sección afectada marcada en {@code degraded} (lista para summary, booleano para charts/news).</p>
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
@Tag(name = "Dashboard", description = "Datos públicos agregados de mercado y noticias")
public class DashboardController {

    private final DashboardService dashboardService;
    private final NewsService newsService;
    private final ChartsService chartsService;

    /**
     * Obtiene noticias normalizadas de IA/tecnología.
     * <p>Protegido por JWT. Usa NewsAPI con fallback a HN Algolia; si ambas fuentes fallan,
     * devuelve {@code 200} con {@code items} vacío o stale y {@code degraded=true}.</p>
     *
     * @param topic tópico IA/tecnología, por defecto ai-tech
     * @param limit máximo de artículos entre uno y ocho
     * @return noticias disponibles con indicador de degradación
     */
    @GetMapping("/news")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Obtener noticias IA/tecnología",
            description = "El fallback HN es normal. degraded solo indica el fallo de ambas fuentes.")
    @ApiResponse(responseCode = "200", description = "Noticias disponibles, frescas o stale",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = NewsResponse.class),
                    examples = @ExampleObject(name = "modoMock", ref = "#/components/examples/NewsMock")))
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
     * Obtiene la serie histórica de un activo soportado.
     * <p>Protegido por JWT. Soporta BTC, ETH, AAPL, MSFT, SPY con rango {@code 7d}.
     * Si el proveedor falla sin caché fresca, devuelve {@code 200} con serie stale o vacía y
     * {@code degraded=true}.</p>
     *
     * @param asset activo soportado obligatorio
     * @param range ventana opcional, únicamente 7d
     * @return serie histórica en USD y estado de degradación del proveedor
     */
    @GetMapping("/charts")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Obtener la serie histórica de un activo",
            description = "Últimos siete días; si falla el proveedor devuelve datos stale o puntos vacíos.")
    @ApiResponse(responseCode = "200", description = "Serie histórica disponible",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ChartsResponse.class),
                    examples = @ExampleObject(name = "modoMock", ref = "#/components/examples/ChartsMock")))
    @ApiResponse(responseCode = "400", description = "Activo o rango no soportado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "JWT ausente, inválido o expirado",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ChartsResponse getCharts(
            @Parameter(description = "Activo soportado", required = true,
                    schema = @Schema(allowableValues = {"BTC", "ETH", "AAPL", "MSFT", "SPY"}))
            @RequestParam(name = "asset") String asset,
            @Parameter(description = "Ventana histórica", schema = @Schema(allowableValues = "7d"))
            @RequestParam(name = "range", required = false) String range) {
        return chartsService.getCharts(asset, range);
    }

    /**
     * Devuelve el resumen agregado del dashboard.
     * <p>Protegido por JWT. Agrega dólar ARS ({@code fx.quotes.usd_oficial/usd_blue}), criptomonedas
     * ({@code crypto.values.bitcoin/ethereum}) y bolsa ({@code equities.values.AAPL/MSFT/SPY}).
     * Los fallos de proveedores individuales se informan en {@code degraded} sin generar {@code 5xx}.
     * Solo un fallo inesperado de la orquestación devuelve {@code 500}.</p>
     *
     * @return {@code 200} con los datos disponibles y la lista de secciones sin datos frescos;
     *         {@code 401} si el JWT es ausente o inválido; {@code 500} con el contrato de error
     *         si falla la orquestación completa
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
