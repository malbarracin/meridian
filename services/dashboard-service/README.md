# dashboard-service — datos públicos del dashboard

Servicio REST que agrega en una sola respuesta las cotizaciones de dólar oficial y blue, los tipos de
cambio USD→EUR/BRL, los precios e histórico de criptomonedas, las quotes y velas de bolsa y las noticias
de IA/tecnología. Lo consume el frontend del dashboard de Meridian; no persiste datos: consulta proveedores
externos, cachea en memoria y degrada las secciones que fallan sin cortar la respuesta.

## Funcionalidades

- `GET /api/v1/dashboard`: devuelve todas las secciones agregadas (dólar, FX, cripto, histórico, bolsa,
  velas y noticias) y la lista `degraded` con las secciones sin datos frescos.
- `GET /actuator/health` y `GET /actuator/info`: estado y metadatos del servicio.
- Swagger UI y OpenAPI: contrato interactivo en `/swagger-ui.html` y `/v3/api-docs`.
- Modo mock (`DASHBOARD_USE_MOCK=true`): fixtures estáticas sin llamadas externas, para desarrollo y QA.
- Caché en memoria con TTL configurable (mínimo 60 s para cripto) y última respuesta exitosa ante fallos.

## Stack

| | |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.5.16 (Spring MVC + WebClient) |
| Base de datos | Ninguna; caché en memoria |
| Build | Maven (wrapper incluido) |
| Documentación API | springdoc-openapi 2.8.17 (Swagger UI) |
| Calidad | JUnit 5, AssertJ, MockWebServer, JaCoCo (80 % de líneas) |

## Requisitos

- JDK 21 (`java -version`).
- Acceso a internet solo para el modo con proveedores reales.
- No requiere Docker, base de datos ni variables obligatorias para arrancar.

## Cómo correrlo

### Local

Desde esta carpeta (`services/dashboard-service`):

```powershell
.\mvnw.cmd spring-boot:run
```

En Linux/macOS: `./mvnw spring-boot:run`. El servicio escucha en `http://localhost:8081`.

Para arrancar sin llamadas externas:

```powershell
$env:DASHBOARD_USE_MOCK = 'true'
.\mvnw.cmd spring-boot:run
```

Con el servicio arriba:

```powershell
Invoke-RestMethod http://localhost:8081/actuator/health
Invoke-RestMethod http://localhost:8081/api/v1/dashboard
```

El puerto se cambia con `PORT` (por ejemplo `$env:PORT = '9090'`). Si se cambia el puerto, ajustar
`BASE_URL` en el entorno de Postman y `BASE_URL` en `delivery/bruno/dashboard-service/environments/local.bru`.

### Con Docker

DevOps mantiene el Dockerfile, Compose y Helm del repositorio. Ver la sección de dashboard-service en
`delivery/devops-local.md`. Para probar la API no hace falta Docker.

## Configuración

Todas las variables son opcionales. Los valores entre paréntesis son los de `application.yml`.

| Variable / propiedad | Para qué | Valor por defecto |
|---|---|---|
| `PORT` | Puerto HTTP (`server.port`) | `8081` |
| `DASHBOARD_USE_MOCK` | Sirve fixtures estáticas sin llamadas externas | `false` |
| `DASHBOARD_CACHE_TTL_SECONDS` | TTL de la caché por recurso; cripto usa mínimo 60 s | `60` |
| `FINNHUB_API_KEY` | Clave de Finnhub para quotes y velas; si falta se usan fixtures | vacía |
| `NEWS_API_KEY` | Clave de NewsAPI; si falta se consulta HN Algolia | vacía |
| `DASHBOARD_DOLAR_API_BASE_URL` | Base URL de DolarApi | `https://dolarapi.com` |
| `DASHBOARD_FRANKFURTER_BASE_URL` | Base URL de Frankfurter | `https://api.frankfurter.app` |
| `DASHBOARD_COIN_GECKO_BASE_URL` | Base URL de CoinGecko | `https://api.coingecko.com` |
| `DASHBOARD_FINNHUB_BASE_URL` | Base URL de Finnhub | `https://finnhub.io` |
| `DASHBOARD_NEWS_API_BASE_URL` | Base URL de NewsAPI | `https://newsapi.org` |
| `DASHBOARD_HN_ALGOLIA_BASE_URL` | Base URL del fallback de noticias HN Algolia | `https://hn.algolia.com` |
| `DASHBOARD_<PROVEEDOR>_TIMEOUT_SECONDS` | Timeout de conexión y respuesta de cada proveedor | `5` |

Los nombres exactos de `<PROVEEDOR>` son `DOLAR_API`, `FRANKFURTER`, `COIN_GECKO`, `FINNHUB`, `NEWS_API`
y `HN_ALGOLIA`. Las claves y las URLs se leen del entorno; no se versionan valores reales. El archivo
`.env.example` documenta las variables opcionales.

### Proveedores

| Sección | Proveedor | Qué consulta | Sin clave / caído |
|---|---|---|---|
| `dolar` | DolarApi | `/v1/dolares`, filtra oficial y blue | Se degrada `dolar`; sin caché previa, lista vacía |
| `fx` | Frankfurter | `/latest?from=USD&to=EUR,BRL` | Se degrada `fx` |
| `crypto` | CoinGecko | `/api/v3/simple/price` para BTC y ETH | Se degrada `crypto` |
| `cryptoHistory` | CoinGecko | `/api/v3/coins/{id}/market_chart?days=7` | Se degrada `cryptoHistory` |
| `equities` | Finnhub | `/api/v1/quote` para AAPL, MSFT y SPY | Sin `FINNHUB_API_KEY` usa fixtures estáticas |
| `equityCandles` | Finnhub | `/api/v1/stock/candle` diario, últimos 7 días | Sin `FINNHUB_API_KEY` usa fixtures estáticas |
| `news` | NewsAPI → HN Algolia | `/v2/everything` y fallback `/api/v1/search` | Sin `NEWS_API_KEY` usa HN; si ambos fallan, se degrada `news` |

Cada adaptador propaga sus errores al agregador, que ejecuta las consultas en paralelo con hilos virtuales
y aísla cada sección. Un fallo de proveedor no genera un 5xx: la sección queda en `degraded` y, si había una
respuesta anterior en caché, se sirve esa copia. Solo un fallo inesperado de la orquestación devuelve
`500 INTERNAL_ERROR`.

Limitación conocida de Finnhub: la documentación pública de
[quotes](https://finnhub.io/docs/api/quote), [candles](https://finnhub.io/docs/api/stock-candles) y
[planes](https://finnhub.io/pricing) no permitió confirmar desde este entorno la disponibilidad de SPY con
una cuenta free. No se verificaron llamadas reales con clave ni se eligió un ticker alternativo sin
evidencia; SPY se conserva como ETF de referencia. Los tests usan MockWebServer y no consumen cuota.

## API

- Swagger UI: http://localhost:8081/swagger-ui.html
- OpenAPI: http://localhost:8081/v3/api-docs · grupo `dashboard`: `/v3/api-docs/dashboard`
- Copia estática generada por el build: `delivery/openapi/dashboard-service/openapi.json` y `.yaml`

| Método | Ruta | Qué hace | Respuestas |
|---|---|---|---|
| GET | `/api/v1/dashboard` | Agrega todas las secciones del dashboard | `200` `DashboardResponse`, `500` `ErrorResponse` |
| GET | `/actuator/health` | Estado del servicio | `200` `{"status":"UP"}` |
| GET | `/actuator/info` | Metadatos de la aplicación | `200` |

```bash
curl.exe -i http://localhost:8081/api/v1/dashboard
```

Con el servicio en modo mock la respuesta usa fixtures; con proveedores reales cambian los valores y
`degraded` puede traer secciones. Respuesta `200` en modo mock (extracto):

```json
{
  "dolar": [
    { "tipo": "oficial", "compra": 1000.00, "venta": 1040.00 },
    { "tipo": "blue", "compra": 1200.00, "venta": 1220.00 }
  ],
  "fx": [
    { "from": "USD", "to": "EUR", "rate": 0.92 },
    { "from": "USD", "to": "BRL", "rate": 5.10 }
  ],
  "crypto": [
    { "id": "bitcoin", "symbol": "BTC", "priceUsd": 67000, "change24h": 2.5 },
    { "id": "ethereum", "symbol": "ETH", "priceUsd": 3500, "change24h": -1.2 }
  ],
  "cryptoHistory": [
    { "timestamp": "2026-10-15T00:00:00Z", "priceUsd": 67000 }
  ],
  "equities": [
    { "ticker": "AAPL", "price": 175.00, "currency": "USD" },
    { "ticker": "MSFT", "price": 415.00, "currency": "USD" },
    { "ticker": "SPY", "price": 450.00, "currency": "USD" }
  ],
  "equityCandles": [
    {
      "ticker": "AAPL",
      "ohlcPoints": [
        { "t": 1792627200, "o": 175.00, "h": 175.00, "l": 175.00, "c": 175.00, "v": 0 }
      ]
    }
  ],
  "news": [
    {
      "id": "demo-ai",
      "title": "AI research demo",
      "summary": "Static AI news fixture",
      "source": "Meridian Demo",
      "publishedAt": "2026-01-01T12:00:00Z",
      "url": "https://example.com/demo-ai",
      "topic": "ai-tech"
    }
  ],
  "degraded": []
}
```

`degraded` contiene los nombres de sección (`dolar`, `fx`, `crypto`, `cryptoHistory`, `equities`,
`equityCandles`, `news`). `cryptoHistory` se informa una sola vez aunque fallen BTC y ETH. Las listas
pueden venir vacías si no hay datos ni caché previa; `null` no se devuelve.

### Errores

El contrato es uniforme para toda la API (`com.meridian.dashboard.exception`): `code`, `message`, `details`
y `timestamp`; `details` nunca es `null` y no se expone stacktrace. El endpoint del dashboard solo puede
devolver `500 INTERNAL_ERROR` en un fallo inesperado de la orquestación:

```json
{
  "code": "INTERNAL_ERROR",
  "message": "Error interno",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

| HTTP | `code` | Cuándo |
|---|---|---|
| 400 | `VALIDATION_ERROR` | Datos inválidos (reservado para endpoints con body o parámetros) |
| 400 | `BAD_REQUEST` | Solicitud mal formada (reservado) |
| 404 | `NOT_FOUND` | Recurso inexistente (reservado) |
| 409 | `CONFLICT` | Conflicto de dominio (reservado) |
| 500 | `INTERNAL_ERROR` | Fallo inesperado de la orquestación o del handler |

El `GlobalExceptionHandler` mapea los cinco códigos; hoy `GET /api/v1/dashboard` no recibe body ni
parámetros, por lo que en la práctica solo aplica `500`.

## Probar la API

- Postman: importar `delivery/postman/dashboard-service.postman_collection.json` y el entorno
  `delivery/postman/dashboard-service.postman_environment.json`. El entorno trae `BASE_URL`,
  `FINNHUB_API_KEY`, `NEWS_API_KEY`, `DASHBOARD_CACHE_TTL_SECONDS` y `DASHBOARD_USE_MOCK`.
- Bruno: abrir la colección `delivery/bruno/dashboard-service/` o importar el JSON de Postman.
- Paso a paso, orden y resultados esperados: `delivery/qa-manual.md`.
- Ejemplos curl: `delivery/api.md`.
- OpenAPI: `delivery/openapi/dashboard-service/openapi.json` y `.yaml`.

## Tests

Desde esta carpeta:

```powershell
.\mvnw.cmd -q -B verify
```

Compila, corre los tests y falla si la cobertura de líneas baja del 80 % (JaCoCo). Al final del build,
`OpenApiExportTest` regenera `delivery/openapi/dashboard-service/openapi.json` y `.yaml`.

| Área | Tests |
|---|---|
| Agregación y caché | `DashboardServiceTest`, `DashboardCacheServiceTest` |
| Adaptadores | `DolarApiAdapterTest`, `FrankfurterAdapterTest`, `CoinGeckoAdapterTest`, `FinnhubAdapterTest`, `NewsAdapterTest` (MockWebServer) |
| Controller e integración | `DashboardControllerTest`, `DashboardMockIntegrationTest` |
| Contrato de error | `ErrorResponseTest`, `ErrorContractHttpTest` |
| Configuración | `DashboardApplicationTest`, `WebClientConfigTest`, `OpenApiConfigTest` |
| OpenAPI | `OpenApiExportTest` |

No requieren Docker ni claves reales: los adaptadores usan MockWebServer y el modo mock está activo en los
tests de integración.

## Estructura del proyecto

```text
com.meridian.dashboard
├── DashboardApplication.java
├── config/                  # WebClient por proveedor, executor virtual, OpenAPI
├── exception/               # contrato de error: ErrorResponse, ErrorCode, handler
└── dashboard/
    ├── controller/          # DashboardController
    ├── service/             # DashboardService (orquestación en paralelo)
    ├── cache/               # DashboardCacheService (TTL + última respuesta buena)
    ├── adapter/             # DolarApi, Frankfurter, CoinGecko, Finnhub, News/HN
    ├── mapper/              # MapStruct: respuestas de proveedor → DTO
    ├── mock/                # DashboardMockFixtures
    └── dto/                 # DashboardResponse y DTOs de cada sección
```

## Documentación

| Documento | Para qué |
|---|---|
| `delivery/api.md` | Uso de la API con curl (sección dashboard-service) |
| `delivery/architecture-overview.md` | Capas, decisiones y flujo de una petición (sección dashboard-service) |
| `delivery/qa-manual.md` | Cómo probar todo con Postman/Bruno (sección dashboard-service) |
| `delivery/openapi/dashboard-service/` | Contrato OpenAPI versionado generado por el build |
| `delivery/devops-local.md` | Docker, CI y despliegue (sección dashboard-service) |
| `specs/dashboard/apis-publicas.md` | Requisitos funcionales de las APIs públicas |
| `specs/dashboard/arquitectura-apis-publicas.md` | Decisiones de arquitectura |
