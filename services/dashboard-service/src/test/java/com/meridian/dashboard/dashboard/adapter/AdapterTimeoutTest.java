package com.meridian.dashboard.dashboard.adapter;

import java.time.Duration;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import com.meridian.dashboard.dashboard.mapper.CryptoMapper;
import com.meridian.dashboard.dashboard.mapper.CurrencyMapper;
import com.meridian.dashboard.dashboard.mapper.EquityMapper;
import com.meridian.dashboard.dashboard.mapper.NewsMapper;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Timeout configurable en todos los adapters")
class AdapterTimeoutTest {

    @ParameterizedTest(name = "{0}: acepta respuestas después de cinco segundos")
    @MethodSource("providers")
    @DisplayName("Respeta un timeout configurado superior a cinco segundos")
    void acceptsResponseBeyondFiveSeconds(String provider, String body, Function<WebClient, Object> fetch) {
        WebClient client = WebClient.builder().exchangeFunction(request -> Mono.delay(Duration.ofMillis(5200))
                .map(ignored -> ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                        .body(body).build())).build();

        assertThat(fetch.apply(client)).isNotNull();
    }

    private static Stream<Arguments> providers() {
        CurrencyMapper currencies = Mappers.getMapper(CurrencyMapper.class);
        NewsMapper news = Mappers.getMapper(NewsMapper.class);
        return Stream.of(
                Arguments.of("dolar-api", """
                        [{"casa":"oficial","compra":1000,"venta":1050},
                         {"casa":"blue","compra":1100,"venta":1150}]
                        """, (Function<WebClient, Object>) client ->
                        new DolarApiAdapter(client, currencies, false, 8).fetchDolar()),
                Arguments.of("frankfurter", """
                        {"base":"USD","rates":{"EUR":0.9,"BRL":5.0}}
                        """, (Function<WebClient, Object>) client ->
                        new FrankfurterAdapter(client, currencies, false, 8).fetchFx()),
                Arguments.of("coin-gecko", """
                        {"bitcoin":{"usd":67000,"usd_24h_change":2},
                         "ethereum":{"usd":3500,"usd_24h_change":1}}
                        """, (Function<WebClient, Object>) client -> new CoinGeckoAdapter(client,
                        Mappers.getMapper(CryptoMapper.class), new MockEnvironment()
                                .withProperty("dashboard.providers.coin-gecko.timeout-seconds", "8")).fetchPrices()),
                Arguments.of("finnhub", """
                        {"s":"ok","t":[100],"o":[1],"h":[2],"l":[1],"c":[2],"v":[10]}
                        """, (Function<WebClient, Object>) client -> new FinnhubAdapter(client,
                        Mappers.getMapper(EquityMapper.class), "test-key", false, 8).fetchCandles("AAPL")),
                Arguments.of("news-api", "{\"status\":\"ok\",\"articles\":[]}",
                        (Function<WebClient, Object>) client -> new NewsAdapter(client, unavailableFallback(),
                                news, "test-key", false, 8, 1).fetchNews()),
                Arguments.of("hn-algolia", "{\"hits\":[]}",
                        (Function<WebClient, Object>) client -> new NewsAdapter(unavailableFallback(), client,
                                news, "", false, 1, 8).fetchNews()));
    }

    private static WebClient unavailableFallback() {
        return WebClient.builder().exchangeFunction(request ->
                Mono.error(new IllegalStateException("Proveedor no esperado"))).build();
    }
}
