package com.meridian.dashboard.dashboard.adapter;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.meridian.dashboard.dashboard.dto.FxRateResponse;
import com.meridian.dashboard.dashboard.mapper.CurrencyMapper;
import com.meridian.dashboard.dashboard.mock.DashboardMockFixtures;

/** Consulta Frankfurter; propaga errores para que el agregador degrade la sección FX. */
@Component
public class FrankfurterAdapter {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
    private static final List<String> TARGET_CURRENCIES = List.of("EUR", "BRL");

    private final WebClient webClient;
    private final CurrencyMapper mapper;
    private final boolean useMock;

    public FrankfurterAdapter(@Qualifier("frankfurterWebClient") WebClient webClient,
            CurrencyMapper mapper, @Value("${dashboard.use-mock:false}") boolean useMock) {
        this.webClient = webClient;
        this.mapper = mapper;
        this.useMock = useMock;
    }

    /** @return tasas USD a EUR y BRL; lanza excepción ante timeout, error HTTP o respuesta incompleta. */
    public List<FxRateResponse> fetchFx() {
        if (useMock) {
            return DashboardMockFixtures.getFx();
        }
        FrankfurterRates quotes = webClient.get()
                .uri(builder -> builder.path("/latest").queryParam("from", "USD")
                        .queryParam("to", "EUR,BRL").build())
                .retrieve().bodyToMono(FrankfurterRates.class).timeout(REQUEST_TIMEOUT).block();
        if (quotes == null || !"USD".equals(quotes.base()) || quotes.rates() == null
                || TARGET_CURRENCIES.stream().anyMatch(currency -> quotes.rates().get(currency) == null)) {
            throw new IllegalStateException("Faltan tipos de cambio requeridos");
        }
        return TARGET_CURRENCIES.stream()
                .map(currency -> mapper.toFxRateResponse("USD", currency, quotes.rates().get(currency)))
                .toList();
    }
}
