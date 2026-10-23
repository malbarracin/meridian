# dashboard-service

Base del servicio de APIs públicas de Meridian (Java 21, puerto 8081).

Desde esta carpeta: `./mvnw clean verify` para compilar y probar, y
`./mvnw spring-boot:run` para ejecutar (Windows: `mvnw.cmd`).

Configuración opcional: `PORT` (8081), `DASHBOARD_USE_MOCK` (false),
`DASHBOARD_CACHE_TTL_SECONDS` (60), `FINNHUB_API_KEY` y `NEWS_API_KEY` (vacías).
Cada proveedor acepta `DASHBOARD_<PROVEEDOR>_BASE_URL` y
`DASHBOARD_<PROVEEDOR>_TIMEOUT_SECONDS` (5); los nombres están en application.yml.
Las claves se consumirán en los adapters; no se incorporan a los clientes base.
FinnhubAdapter consume `FINNHUB_API_KEY` únicamente al consultar Finnhub.

Actuator: `/actuator/health`, `/actuator/info`. OpenAPI: `/v3/api-docs/dashboard`;
Swagger UI: `/swagger-ui.html`. El endpoint se implementa en una tarea posterior.

Los adapters DolarApi y Frankfurter consultan dólar oficial/blue y USD→EUR/BRL,
respectivamente, con un límite total de cinco segundos y propagación de errores
al agregador. La caché TTL y la gestión de secciones degraded corresponden a otras tareas.
Con `DASHBOARD_USE_MOCK=true` devuelven fixtures sin llamadas HTTP: dólar oficial
1000/1040 ARS (compra/venta), blue 1200/1220 ARS y tasas USD→EUR 0.92,
USD→BRL 5.10. Son datos ficticios estáticos, no cotizaciones actuales.

Finnhub consulta AAPL, MSFT y SPY en USD mediante `/api/v1/quote` y
`/api/v1/stock/candle` (resolución diaria). Permite solicitar un intervalo Unix
inclusivo; por defecto consulta los últimos siete días. Cada petición tiene un
límite de cinco segundos. Los errores HTTP, timeouts y respuestas incompletas se
propagan para que el agregador degrade `equities` o `equityCandles`.
Sin clave (incluida una clave en blanco) o con modo mock, sirve los fixtures
de `DashboardMockFixtures`: AAPL 175.00, MSFT 415.00 y SPY 450.00 USD;
una vela sintética por ticker a 2026-10-22 00:00 UTC, con OHLC igual al precio y
volumen cero. Estos fixtures son fijos y no representan un histórico real.
Si se indica un rango explícito, también se filtran las velas demo por ese rango.

Se conserva SPY como ETF de referencia. La documentación pública de Finnhub
([quotes](https://finnhub.io/docs/api/quote),
[candles](https://finnhub.io/docs/api/stock-candles),
[planes](https://finnhub.io/pricing)) no permitió confirmar desde este entorno
su disponibilidad con una cuenta free. No se verificaron llamadas reales con
clave ni se eligió un ticker alternativo sin evidencia de disponibilidad.
Una denegación de acceso se propaga como fallo del proveedor; con clave configurada
no se oculta con fixtures. Los tests usan MockWebServer y no consumen cuota.
