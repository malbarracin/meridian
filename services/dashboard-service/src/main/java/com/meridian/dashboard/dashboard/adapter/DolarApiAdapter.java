package com.meridian.dashboard.dashboard.adapter;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.meridian.dashboard.dashboard.dto.DolarResponse;
import com.meridian.dashboard.dashboard.mapper.CurrencyMapper;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

/** Consulta DolarApi; propaga errores para que el agregador degrade la sección dólar. */
@Component
public class DolarApiAdapter {

    private final WebClient webClient;
    private final CurrencyMapper mapper;
    private final boolean useMock;
    private final Duration requestTimeout;

    public DolarApiAdapter(@Qualifier("dolarApiWebClient") WebClient webClient,
            CurrencyMapper mapper, @Value("${dashboard.use-mock:false}") boolean useMock,
            @Value("${dashboard.providers.dolar-api.timeout-seconds:5}") long timeoutSeconds) {
        this.webClient = webClient;
        this.mapper = mapper;
        this.useMock = useMock;
        this.requestTimeout = Duration.ofSeconds(timeoutSeconds);
    }

    /** @return cotizaciones oficial y blue; lanza excepción si el proveedor falla o devuelve datos incompletos. */
    public List<DolarResponse> fetchDolar() {
        if (useMock) {
            return DashboardMockFixtures.getDolar();
        }
        List<DolarResponse> quotes = webClient.get().uri("/v1/dolares")
                .retrieve().bodyToFlux(DolarQuote.class)
                .filter(quote -> "oficial".equals(quote.casa()) || "blue".equals(quote.casa()))
                .map(quote -> {
                    if (quote.compra() == null || quote.venta() == null) {
                        throw new IllegalStateException("Cotización de dólar incompleta");
                    }
                    return mapper.toDolarResponse(quote);
                })
                .collectList().timeout(requestTimeout).block();
        if (quotes == null || quotes.stream().noneMatch(quote -> "oficial".equals(quote.tipo()))
                || quotes.stream().noneMatch(quote -> "blue".equals(quote.tipo()))) {
            throw new IllegalStateException("Faltan cotizaciones de dólar requeridas");
        }
        return List.copyOf(quotes);
    }
}
