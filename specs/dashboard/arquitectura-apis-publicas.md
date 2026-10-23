---
title: "Dashboard Service — Arquitectura (H8): Catálogo de APIs públicas"
status: borrador
source: specs/dashboard/apis-publicas.md
updated: 2026-10-22T23:09:31.272Z
---

## Fuente

- Especificación funcional: `specs/dashboard/apis-publicas.md` (H8 — Catálogo APIs públicas Dashboard Meridian)
- `project.yaml`: `java.group_id: com.meridian`, `java.base_package: com.meridian.users` (auth-service)
- Arquitectura de referencia: `specs/auth/arquitectura.md` (patrón de capas del proyecto)

---

## 1. Identidad del servicio

| Parámetro | Valor | Fuente |
|-----------|-------|--------|
| Carpeta | `services/dashboard-service` | nueva — H8 define un servicio independiente |
| `groupId` | `com.meridian` | `project.yaml java.group_id` |
| `artifactId` | `dashboard-service` | spec: "dashboard-service" |
| `base_package` | `com.meridian.dashboard` | `group_id` + artifact (patrón del proyecto; `java.base_package` de `project.yaml` pertenece al auth-service) |
| Clase principal | `DashboardApplication` | `base_package` + convención `*Application` |
| Java | 21 | default (no en `project.yaml java.version`) |
| Spring Boot | 3.x última estable | default |
| HTTP cliente | `spring-boot-starter-webflux` (solo `WebClient`) | adapters consumen APIs externas de forma no bloqueante; no expone endpoints reactivos — la API propia sigue siendo MVC/Servlet |
| Persistencia | **ninguna** | no hay modelo de datos persistido; cache en memoria |
| Puerto | 8081 | evitar colisión con auth-service (8080) |
| API versioning | **deshabilitado** | `api.versioning` ausente en `project.yaml` |

> **Nota sobre `base_package`**: `project.yaml` declara `java.base_package: com.meridian.users`, que es la identidad del auth-service ya existente. El dashboard-service es un servicio independiente; su `base_package` se deriva del `group_id` `com.meridian` más el artifact `dashboard`, siguiendo el mismo patrón. Si la persona prefiere otra convención, actualizar `project.yaml` con `java.dashboard_base_package` antes de implementar.

---

## 2. Árbol de paquetes

```
services/dashboard-service/
├── pom.xml
├── mvnw / mvnw.cmd / .mvn/wrapper/
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── java/com/meridian/dashboard/
    │   │   ├── DashboardApplication.java
    │   │   ├── config/
    │   │   │   ├── WebClientConfig.java          # beans WebClient por proveedor (timeout, base URL)
    │   │   │   └── OpenApiConfig.java            # springdoc GroupedOpenApi
    │   │   ├── exception/                        # contrato de error transversal
    │   │   │   ├── ErrorResponse.java            # record
    │   │   │   ├── FieldError.java               # record
    │   │   │   ├── ErrorCode.java                # enum: VALIDATION_ERROR, BAD_REQUEST, NOT_FOUND, CONFLICT, INTERNAL_ERROR
    │   │   │   ├── GlobalExceptionHandler.java   # @RestControllerAdvice
    │   │   │   ├── NotFoundException.java        # base abstracta (RuntimeException)
    │   │   │   └── ConflictException.java        # base abstracta (RuntimeException)
    │   │   └── dashboard/
    │   │       ├── controller/
    │   │       │   └── DashboardController.java  # @RestController @RequestMapping("/api/v1/dashboard")
    │   │       ├── service/
    │   │       │   └── DashboardService.java     # agrega respuestas de todos los adapters
    │   │       ├── adapter/
    │   │       │   ├── DolarApiAdapter.java      # DolarApi — cotizaciones ARS
    │   │       │   ├── FrankfurterAdapter.java   # Frankfurter ECB — FX internacional (opcional)
    │   │       │   ├── CoinGeckoAdapter.java     # CoinGecko — precio y serie cripto
    │   │       │   ├── FinnhubAdapter.java       # Finnhub — quote + velas (equities)
    │   │       │   └── NewsAdapter.java          # NewsAPI con fallback a HN Algolia
    │   │       ├── cache/
    │   │       │   └── DashboardCacheService.java # cache en memoria (ConcurrentHashMap + TTL) + degraded
    │   │       ├── dto/
    │   │       │   ├── DashboardResponse.java    # record: dolar, fx, crypto, equities, news, degraded[]
    │   │       │   ├── DolarResponse.java        # record: tipo, compra, venta
    │   │       │   ├── FxRateResponse.java       # record: from, to, rate
    │   │       │   ├── CryptoResponse.java       # record: id, symbol, priceUsd, change24h
    │   │       │   ├── CryptoPricePoint.java     # record: timestamp, priceUsd
    │   │       │   ├── EquityQuoteResponse.java  # record: ticker, price, currency
    │   │       │   ├── CandleResponse.java       # record: ticker, ohlcPoints[]
    │   │       │   ├── OhlcPoint.java            # record: t, o, h, l, c, v
    │   │       │   └── NewsArticleResponse.java  # record: id, title, summary, source, publishedAt, url, topic
    │   │       └── mock/
    │   │           └── DashboardMockFixtures.java # fixtures estáticas para DASHBOARD_USE_MOCK=true
    │   └── resources/
    │       └── application.yml
    └── test/
        ├── java/com/meridian/dashboard/
        │   ├── DashboardApplicationTest.java     # contextLoads
        │   └── dashboard/
        │       ├── controller/
        │       │   └── DashboardControllerTest.java   # @WebMvcTest + mock DashboardService
        │       ├── service/
        │       │   └── DashboardServiceTest.java      # @ExtendWith(MockitoExtension)
        │       ├── adapter/
        │       │   ├── DolarApiAdapterTest.java        # MockWebServer
        │       │   ├── CoinGeckoAdapterTest.java       # MockWebServer
        │       │   ├── FinnhubAdapterTest.java         # MockWebServer
        │       │   └── NewsAdapterTest.java            # MockWebServer
        │       └── cache/
        │           └── DashboardCacheServiceTest.java  # JUnit 5
        └── resources/
            └── application.yml                   # config de tests (mock mode, sin keys)
```

> **Regla de paquetes**: estructura por subpaquetes bajo el módulo `dashboard`. La capa `adapter/` reemplaza a `repository/` (no hay persistencia); `cache/` es la capa de almacenamiento en memoria. Prohibido paquete plano (`dashboard.DashboardController` sin subcapa).

---

## 3. Módulo de dominio: `dashboard`

### 3.1 Responsabilidades por capa

| Capa | Paquete | Responsabilidad |
|------|---------|-----------------|
| Controller | `dashboard.controller` | HTTP, parámetros, `@Valid`, status. Devuelve `DashboardResponse`. |
| Service | `dashboard.service` | Orquesta adapters en paralelo; agrega resultados; marca secciones degraded. |
| Adapter | `dashboard.adapter` | Llama a proveedor externo vía `WebClient`. Devuelve datos del dominio o lanza excepción (timeout, error HTTP). Un adapter por proveedor. |
| Cache | `dashboard.cache` | Guarda la última respuesta exitosa de cada adapter con TTL; sirve stale si el proveedor falla con TTL vencido. |
| DTO | `dashboard.dto` | Modelo normalizado de datos (contratos internos + respuesta API pública). |
| Mock | `dashboard.mock` | Fixtures estáticas cargadas en memoria; devuelve datos fijos cuando `DASHBOARD_USE_MOCK=true`. |

### 3.2 Modelo `DashboardResponse`

```java
public record DashboardResponse(
    List<DolarResponse>       dolar,
    List<FxRateResponse>      fx,
    List<CryptoResponse>      crypto,
    List<CryptoPricePoint>    cryptoHistory,  // serie 7d (BTC + ETH)
    List<EquityQuoteResponse> equities,
    List<CandleResponse>      equityCandles,
    List<NewsArticleResponse> news,
    List<String>              degraded        // secciones con falla: "dolar", "crypto", "equities", "news"
) {}
```

- `degraded` es una lista de strings con los nombres de sección que fallaron. Nunca `null`; vacía si todo está bien.
- Los campos de sección pueden ser listas vacías si la sección está degraded.

### 3.3 Proveedores y adapters

| Adapter | URL base | Autenticación | Obligatorio |
|---------|----------|---------------|-------------|
| `DolarApiAdapter` | `https://dolarapi.com` | ninguna | sí |
| `FrankfurterAdapter` | `https://api.frankfurter.app` | ninguna | no (opcional) |
| `CoinGeckoAdapter` | `https://api.coingecko.com` | ninguna (free tier) | sí |
| `FinnhubAdapter` | `https://finnhub.io` | `token` query param — `FINNHUB_API_KEY` | sí (fixture si sin key) |
| `NewsAdapter` | `https://newsapi.org` (primario) + `https://hn.algolia.com` (fallback) | `X-Api-Key` — `NEWS_API_KEY` | sí (fallback si sin key) |

### 3.4 Fixtures de equities (modo demo / sin key Finnhub)

Cuando `FINNHUB_API_KEY` no está configurada o `DASHBOARD_USE_MOCK=true`:

| Ticker | Precio (fixture) |
|--------|-----------------|
| AAPL | 175.00 USD |
| MSFT | 415.00 USD |
| SPY | 450.00 USD |

> Son valores estáticos, documentados en `DashboardMockFixtures`. El dev no debe inventar precios dinámicos.

---

## 4. Endpoint expuesto

No hay `api.versioning.enabled` → todos los endpoints bajo `/api/v1/`.

### GET `/api/v1/dashboard`

| | |
|---|---|
| **Controller** | `DashboardController.getDashboard()` |
| **Request** | ninguno (sin parámetros obligatorios en MVP) |
| **Response exitosa** | `200 OK` + `DashboardResponse` |
| **Content-Type** | `application/json` |

**Tabla de errores:**

| Caso | HTTP | `code` | Excepción |
|------|------|--------|-----------|
| Error inesperado en orquestación | 500 | `INTERNAL_ERROR` | `Exception` |

> Los fallos de proveedores individuales **no** generan 5xx: se reflejan en `DashboardResponse.degraded`. El endpoint devuelve siempre `200 OK` mientras pueda servir al menos datos parciales o cacheados.

---

## 5. Flujo de datos

```
GET /api/v1/dashboard
  → DashboardController.getDashboard()
      → DashboardService.getDashboard()
          Ejecuta en paralelo (CompletableFuture o llamadas WebClient concurrentes):
            DolarApiAdapter.fetchDolar()      → List<DolarResponse>   (o "dolar" en degraded)
            FrankfurterAdapter.fetchFx()      → List<FxRateResponse>  (o "fx" en degraded)
            CoinGeckoAdapter.fetchPrices()    → List<CryptoResponse>  (o "crypto" en degraded)
            CoinGeckoAdapter.fetchHistory()   → List<CryptoPricePoint>(o "cryptoHistory" en degraded)
            FinnhubAdapter.fetchQuotes()      → List<EquityQuoteResponse> (o "equities" en degraded)
            FinnhubAdapter.fetchCandles()     → List<CandleResponse>  (o "equityCandles" en degraded)
            NewsAdapter.fetchNews()           → List<NewsArticleResponse>(o "news" en degraded)
          Cada adapter:
            1. Si DASHBOARD_USE_MOCK=true → devuelve fixture de DashboardMockFixtures
            2. Consulta DashboardCacheService → si hay entrada válida (dentro de TTL) → devuelve cached
            3. Llama proveedor externo vía WebClient (timeout 3–5 s)
            4. Si éxito → guarda en cache → devuelve datos
            5. Si timeout/error → intenta devolver stale (cache vencida con indicador)
                                → si no hay stale → agrega sección a degraded, devuelve lista vacía
          Agrega resultados en DashboardResponse (degraded = secciones fallidas)
      ← DashboardResponse
  ← 200 OK + DashboardResponse
```

---

## 6. Cache y modo degraded

### 6.1 Estrategia de cache

- `DashboardCacheService` usa un `ConcurrentHashMap<String, CacheEntry>` en memoria.
- Cada `CacheEntry` guarda el dato serializado (o el objeto) y el `Instant` de la última actualización.
- TTL por defecto: `DASHBOARD_CACHE_TTL_SECONDS` (default: 60 s). CoinGecko respeta TTL ≥ 60 s siempre.
- Si el TTL no expiró: se sirve el dato sin llamar al proveedor.
- Si el TTL expiró y el proveedor responde: actualiza la cache.
- Si el TTL expiró y el proveedor falla: sirve el dato stale (con flag interno); la sección se marca `degraded`.

### 6.2 Modo degraded

- Cada sección (dolar, fx, crypto, cryptoHistory, equities, equityCandles, news) se maneja de forma independiente.
- Un fallo en un adapter no afecta a los demás.
- `DashboardResponse.degraded` lista las secciones que no pudieron servirse con datos frescos.

### 6.3 Modo mock (`DASHBOARD_USE_MOCK=true`)

- Todos los adapters delegan a `DashboardMockFixtures` antes de cualquier llamada HTTP.
- No se realiza ninguna llamada a proveedores externos.
- `degraded` siempre vacío en modo mock.

---

## 7. Configuración (`application.yml`)

```yaml
spring:
  application:
    name: dashboard-service
  main:
    web-application-type: servlet   # MVC, no reactivo

server:
  port: ${PORT:8081}
  error:
    include-stacktrace: never
    include-message: never

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      probes:
        enabled: true

springdoc:
  api-docs.path: /v3/api-docs
  swagger-ui.path: /swagger-ui.html

dashboard:
  cache:
    ttl-seconds: ${DASHBOARD_CACHE_TTL_SECONDS:60}
  use-mock: ${DASHBOARD_USE_MOCK:false}
  providers:
    dolar-api:
      base-url: https://dolarapi.com
      timeout-seconds: 5
    frankfurter:
      base-url: https://api.frankfurter.app
      timeout-seconds: 5
    coin-gecko:
      base-url: https://api.coingecko.com
      timeout-seconds: 5
    finnhub:
      base-url: https://finnhub.io
      api-key: ${FINNHUB_API_KEY:}          # vacío = modo fixture
      timeout-seconds: 5
    news-api:
      base-url: https://newsapi.org
      api-key: ${NEWS_API_KEY:}             # vacío = fallback HN Algolia
      timeout-seconds: 5
    hn-algolia:
      base-url: https://hn.algolia.com
      timeout-seconds: 5
```

**Variables de entorno:**

| Variable | Obligatoria | Default | Descripción |
|----------|-------------|---------|-------------|
| `FINNHUB_API_KEY` | no | (vacía) | Key Finnhub; sin ella: fixtures de equities |
| `NEWS_API_KEY` | no | (vacía) | Key NewsAPI; sin ella: fallback a HN Algolia |
| `DASHBOARD_CACHE_TTL_SECONDS` | no | `60` | TTL de cache en segundos |
| `DASHBOARD_USE_MOCK` | no | `false` | Modo demo offline |
| `PORT` | no | `8081` | Puerto del servicio |

> El servicio arranca sin `FINNHUB_API_KEY` ni `NEWS_API_KEY`; ninguna variable es `required` en Spring.

**`application.yml` para tests** (`src/test/resources/application.yml`):

```yaml
spring:
  application:
    name: dashboard-service

dashboard:
  cache:
    ttl-seconds: 60
  use-mock: true   # tests corren en modo mock por defecto; sobreescribir en tests de adapters
  providers:
    dolar-api:
      base-url: http://localhost:${mockserver.port}
      timeout-seconds: 2
    # ... resto de providers apuntando al MockWebServer del test
```

---

## 8. `pom.xml` — dependencias clave

Partir de la plantilla `pom.xml.md` del pack java con estos ajustes:

| Acción | Dependencia |
|--------|-------------|
| **Agregar** | `spring-boot-starter-webflux` (para `WebClient`; sin levantar runtime reactivo en producción) |
| **No agregar** | `spring-boot-starter-data-jpa`, driver de BD, `h2` (sin persistencia) |
| **Agregar** (tests) | `com.squareup.okhttp3:mockwebserver` (scope test) para tests de adapters |
| Mantener | `spring-boot-starter-web`, `spring-boot-starter-validation`, `spring-boot-starter-actuator`, Lombok, MapStruct, springdoc, JaCoCo |

> `spring-boot-starter-webflux` es necesario para `WebClient` aunque el runtime de la app sea Servlet (MVC).
> Spring Boot permite esta combinación; no activa el loop reactivo de Netty si `spring-boot-starter-web` está presente.

---

## 9. Estrategia de tests

| Capa | Clase | Tipo | Qué prueba |
|------|-------|------|------------|
| Controller | `DashboardControllerTest` | `@WebMvcTest` + `MockMvc` | GET `/api/v1/dashboard` devuelve 200 + body con campos esperados; degrada correctamente cuando el service devuelve secciones vacías. |
| Service | `DashboardServiceTest` | `@ExtendWith(MockitoExtension)` | Orquestación de adapters; acumula degraded cuando un adapter falla; devuelve datos cacheados en segundo llamado. |
| Adapter — DolarApi | `DolarApiAdapterTest` | JUnit 5 + `MockWebServer` | Mapea correctamente los tipos `oficial` y `blue`; devuelve lista vacía (+ log) en timeout. |
| Adapter — CoinGecko | `CoinGeckoAdapterTest` | JUnit 5 + `MockWebServer` | Precio y variación 24h de BTC y ETH; serie histórica 7d; comportamiento en error 5xx. |
| Adapter — Finnhub | `FinnhubAdapterTest` | JUnit 5 + `MockWebServer` | Quote de AAPL, MSFT, SPY con key; fixture sin key; comportamiento en timeout. |
| Adapter — News | `NewsAdapterTest` | JUnit 5 + `MockWebServer` | NewsAPI devuelve 8 artículos normalizados; fallo NewsAPI → fallback HN Algolia; fallo ambos → degraded. |
| Cache | `DashboardCacheServiceTest` | JUnit 5 | Sirve dato cacheado dentro del TTL; no llama al supplier si hay cache; devuelve stale tras TTL vencido con proveedor caído. |
| Integración | `DashboardApplicationTest` | `@SpringBootTest` | `contextLoads` con `DASHBOARD_USE_MOCK=true` (sin proveedores externos). |

JaCoCo: umbral mínimo **80%** line coverage. Exclusiones sugeridas: `DashboardApplication`, `DashboardMockFixtures` (datos estáticos sin lógica), clases de `config/` sin lógica.

---

## 10. Riesgos y decisiones pendientes para el Dev

| # | Riesgo / decisión | Recomendación |
|---|-------------------|---------------|
| 1 | **`WebClient` en app Servlet**: usar `WebClient` de forma bloqueante (`.block()`) o adoptar `CompletableFuture` + `WebClient` para llamadas paralelas | Preferir `WebClient` + `.block()` en el service (simple, sin reactor en el path de llamada). Si la latencia importa, usar `Mono.zip` para paralelismo. Decidir en implementación; el plan acepta ambas. |
| 2 | **Tercer ticker de Finnhub** (`SPY` vs índice equivalente en free tier) | La spec deja abierta la verificación en el free tier. El dev verifica al momento de implementar; si `SPY` no está disponible, documentar el ticker alternativo en `DashboardMockFixtures` y `README`. |
| 3 | **Cache en memoria** vs Spring Cache / Caffeine | Cache en memoria con `ConcurrentHashMap` es suficiente para el MVP. Si el equipo prefiere `@Cacheable` + Caffeine, es una mejora válida pero no es alcance de H8. |
| 4 | **Modelo normalizado de H3** | La spec indica que H3 define el contrato interno entre adapters y el aggregator. Los DTOs de esta arquitectura son una propuesta; si H3 ya tiene contratos definidos, el dev los usa en lugar de los DTOs propuestos acá. |
| 5 | **`base_package` del dashboard-service** | `project.yaml` solo declara el base_package del auth-service. El valor `com.meridian.dashboard` se deriva del group_id + artifact. Si la persona quiere otro paquete, actualizarlo en `project.yaml` antes de implementar. |
| 6 | **Colisión de puerto** | auth-service usa 8080; este spec propone 8081 para dashboard-service. Si el entorno tiene otro puerto reservado, actualizar `PORT` en `.env` / docker-compose. |

---

## 11. Checklist de arquitectura

- [ ] Identidad del servicio tomada de `project.yaml` y del artifact `dashboard-service`
- [ ] Árbol de paquetes con subpaquetes por capa (`controller/`, `service/`, `adapter/`, `cache/`, `dto/`, `mock/`)
- [ ] Sin paquete plano (prohibido `dashboard.DashboardController` sin subcapa)
- [ ] `GlobalExceptionHandler` en `exception/`; secciones degraded no generan 5xx
- [ ] Todos los endpoints bajo `/api/v1/` (sin versioning activo)
- [ ] Variables de entorno opcionales; servicio arranca sin keys
- [ ] `DASHBOARD_USE_MOCK=true` deshabilita llamadas HTTP externas
- [ ] Tests por capa con `MockWebServer` para adapters; JaCoCo ≥ 80%
- [ ] `./mvnw -q -B verify` pasa (contextLoads + tests con mock mode)
