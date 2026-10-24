---
title: "Dashboard Service — Arquitectura: contrato público (H3) + catálogo de
  APIs (H8)"
status: borrador
source: specs/dashboard/spec.md (H3, aprobada) +
  specs/dashboard/apis-publicas.md (H8, aprobada)
updated: 2026-10-24T00:05:43.819Z
---

## Fuente

- Especificación funcional del contrato público: `specs/dashboard/spec.md` (H3 — **aprobada**). Es la fuente de
  verdad del shape expuesto al cliente; si algo de este documento la contradice, gana H3.
- Catálogo de proveedores externos: `specs/dashboard/apis-publicas.md` (H8 — aprobada). Sigue vigente para
  adapters, cache y modo mock; no cambia en esta revisión salvo lo indicado en §9.
- `project.yaml`: `java.group_id: com.meridian` (el `base_package` del dashboard-service se deriva del
  group_id + artifact, ver nota de §1; `java.base_package` de `project.yaml` es del auth-service).
- Patrón JWT de referencia: `services/auth-service` (`JwtConfig`, `JwtService`, `AuthController./me`) — el
  dashboard-service **valida** tokens emitidos por el auth-service, no los emite.
- Código ya construido en el repo (`services/dashboard-service`, hito H8): scaffold, adapters (`DolarApi`,
  `Frankfurter`, `CoinGecko`, `Finnhub`, `News`), cache en memoria, mock fixtures, un único endpoint
  `GET /api/v1/dashboard` sin JWT. Este documento reemplaza ese borrador del endpoint único.

## 0. Qué cambia respecto a lo ya implementado (resumen para Tech Lead / dev)

La sesión anterior (H8) dejó construido el catálogo de adapters + cache + mock, y un único endpoint
`GET /api/v1/dashboard` **sin JWT** que devuelve un `DashboardResponse` plano (listas `dolar`, `fx`, `crypto`,
`cryptoHistory`, `equities`, `equityCandles`, `news`, `degraded`). La especificación H3 (aprobada, posterior al
borrador) exige un contrato distinto. Cambios obligatorios:

| # | Qué cambia | De → A |
|---|-----------|--------|
| 1 | Endpoints | Uno (`GET /api/v1/dashboard`) → **tres**: `GET /api/v1/dashboard/summary`, `GET /api/v1/dashboard/charts`, `GET /api/v1/dashboard/news` |
| 2 | Autenticación | Sin protección → **JWT obligatorio** (mismo `JWT_SECRET` HS256 que `auth-service`) en los tres endpoints |
| 3 | Shape de `summary` | Listas planas (`dolar`, `fx`, `equityCandles`, …) → objeto con `brand`, `generatedAt`, `fx.quotes.{usd_oficial,usd_blue}`, `crypto.{bitcoin,ethereum}`, `equities.{AAPL,MSFT,SPY}`, `degraded: []` |
| 4 | `charts` y `news` | No existían como endpoints propios → nuevos, con su propio shape (§4.2, §4.3) |
| 5 | FX internacional (Frankfurter) | Se consultaba en el endpoint único → **no se expone** en ningún endpoint H3 (queda implementado pero sin invocar desde la orquestación pública; ver §3.4) |
| 6 | `DashboardResponse` (DTO plano actual) y `DashboardController.getDashboard()` | Quedan **retirados**: no son parte del contrato H3. Dev los reemplaza; no se mantienen "por si acaso" (evitar dos contratos vivos) |
| 7 | Tercer ticker de equities | Pregunta abierta en H8/H3 → **se resuelve acá: `SPY`** (ya es el valor usado en `DashboardMockFixtures`; no se usa `^GSPC`, no disponible en free tier de Finnhub) |
| 8 | Degraded en `charts`/`news` | Pregunta abierta en H3 → **se resuelve acá**: cada uno tiene una sola sección, así que es un booleano `degraded` (no una lista) — ver §4.2–§4.3 |

**`services/dashboard-service/.m2-local` ya no está en el repositorio**: se agregó en el commit
`5c4f2cc` (tester, TASK-44) y se removió en `12b504a` (checkpoint automático de la corrida `20261023-b0e7`,
previo a esta corrida). `git ls-tree -r HEAD` confirma que no hay ningún `.m2-local` rastreado, y
`.gitignore` (raíz) + `services/dashboard-service/.gitignore` + `.dockerignore` ya lo excluyen
(`.m2-local/`). **No hace falta ninguna acción de limpieza adicional**; si una tarea de DevOps quiere
verificarlo, el comando es `git ls-tree -r HEAD -- services/dashboard-service | grep m2-local` (debe no
devolver nada).

---

## 1. Identidad del servicio

| Parámetro | Valor | Fuente |
|-----------|-------|--------|
| Carpeta | `services/dashboard-service` | ya existe (H8) |
| `groupId` | `com.meridian` | `project.yaml java.group_id` |
| `artifactId` | `dashboard-service` | ya existe |
| `base_package` | `com.meridian.dashboard` | ya existe (derivado de group_id + artifact; ver nota de H8) |
| Clase principal | `DashboardApplication` | ya existe |
| Java / Spring Boot | 21 / 3.x | ya existe |
| HTTP cliente externo | `spring-boot-starter-webflux` (solo `WebClient`) | ya existe |
| **Validación JWT** | **nueva dependencia**: `jjwt-api` / `jjwt-impl` / `jjwt-jackson` (misma versión que `auth-service`, hoy `0.12.6`) | el dashboard-service NO agrega `spring-boot-starter-security`: sigue el mismo patrón liviano que `auth-service` (sin filter chain de Spring Security, validación manual con JJWT) para no introducir un stack nuevo sin necesidad |
| Persistencia | ninguna | sin cambios |
| Puerto | 8081 | sin cambios |
| API versioning | deshabilitado (`api.versioning` ausente en `project.yaml`) | todos los endpoints bajo `/api/v1/` sin versión por path |

**Dependencia operativa nueva**: `JWT_SECRET` del dashboard-service **debe ser el mismo valor** que el
`JWT_SECRET` del auth-service (misma clave HMAC-SHA256, el dashboard-service solo valida, no emite). Es una
tarea de DevOps: compartir el secreto entre ambos servicios en `docker-compose.yml`, CI y Helm (ver §9).

---

## 2. Árbol de paquetes (actualizado)

```
services/dashboard-service/
├── pom.xml                                        # + jjwt-api/impl/jackson
└── src/
    ├── main/
    │   ├── java/com/meridian/dashboard/
    │   │   ├── DashboardApplication.java
    │   │   ├── config/
    │   │   │   ├── WebClientConfig.java
    │   │   │   ├── DashboardExecutorConfig.java
    │   │   │   ├── OpenApiConfig.java              # + SecurityScheme bearerAuth, ejemplos nuevos
    │   │   │   └── WebMvcConfig.java               # NUEVO — registra JwtAuthInterceptor en /api/v1/dashboard/**
    │   │   ├── security/                           # NUEVO — transversal, mismo nivel que config/ y exception/
    │   │   │   ├── JwtConfig.java                  # @ConfigurationProperties("jwt"): secret (obligatorio)
    │   │   │   ├── JwtValidator.java                # valida firma HS256 + expiración (sin emitir tokens)
    │   │   │   └── JwtAuthInterceptor.java          # HandlerInterceptor: exige Authorization: Bearer <token>
    │   │   ├── exception/
    │   │   │   ├── ErrorResponse.java / FieldError.java / GlobalExceptionHandler.java
    │   │   │   ├── ErrorCode.java                  # + UNAUTHORIZED
    │   │   │   ├── NotFoundException.java / ConflictException.java
    │   │   │   ├── UnauthorizedException.java      # NUEVO — 401, mensaje "Credenciales inválidas"
    │   │   │   └── UnsupportedParameterException.java  # NUEVO — 400, un solo tipo para asset/range/topic inválidos
    │   │   └── dashboard/
    │   │       ├── controller/
    │   │       │   └── DashboardController.java    # 3 métodos: getSummary / getCharts / getNews
    │   │       ├── service/
    │   │       │   ├── DashboardService.java       # orquesta summary (fx+crypto+equities, SIN Frankfurter/news)
    │   │       │   ├── ChartsService.java           # NUEVO — serie histórica por asset+range
    │   │       │   ├── NewsService.java              # NUEVO — feed de noticias con límite
    │   │       │   └── SupportedAsset.java           # NUEVO — enum BTC/ETH/AAPL/MSFT/SPY + mapeo a id interno
    │   │       ├── adapter/                          # sin cambios de contrato externo (DolarApi, Frankfurter*,
    │   │       │   ├── DolarApiAdapter.java           # CoinGecko, Finnhub, News) — Frankfurter queda implementado
    │   │       │   ├── FrankfurterAdapter.java        # pero deja de invocarse desde la orquestación pública (§3.4)
    │   │       │   ├── CoinGeckoAdapter.java
    │   │       │   ├── FinnhubAdapter.java
    │   │       │   └── NewsAdapter.java
    │   │       ├── cache/
    │   │       │   └── DashboardCacheService.java    # sin cambios
    │   │       ├── dto/
    │   │       │   ├── SummaryResponse.java           # NUEVO — brand, generatedAt, fx, crypto, equities, degraded
    │   │       │   ├── FxSection.java                  # NUEVO — quotes: Map<String, RateQuote>
    │   │       │   ├── RateQuote.java                  # NUEVO — compra, venta
    │   │       │   ├── CryptoSection.java               # NUEVO — Map<String, CryptoQuote>
    │   │       │   ├── CryptoQuote.java                 # NUEVO — priceUsd, change24hPct
    │   │       │   ├── EquitiesSection.java             # NUEVO — Map<String, EquityQuote>
    │   │       │   ├── EquityQuote.java                 # NUEVO — price, currency
    │   │       │   ├── ChartsResponse.java              # NUEVO — asset, range, points, degraded (boolean)
    │   │       │   ├── ChartPoint.java                  # NUEVO — t (Instant), v (BigDecimal)
    │   │       │   ├── NewsResponse.java                 # NUEVO — items, degraded (boolean)
    │   │       │   ├── NewsArticleResponse.java          # existe — se reutiliza tal cual dentro de NewsResponse.items
    │   │       │   ├── DolarResponse.java / FxRateResponse.java / CryptoResponse.java / CryptoPricePoint.java
    │   │       │   ├── EquityQuoteResponse.java / CandleResponse.java / OhlcPoint.java
    │   │       │   └── DashboardResponse.java            # RETIRAR — ya no es el contrato público (§0, fila 6)
    │   │       └── mock/
    │   │           └── DashboardMockFixtures.java        # sin cambios de datos; agrega accesores por sección pública
    │   └── resources/
    │       └── application.yml                            # + bloque jwt.secret
    └── test/
        ├── java/com/meridian/dashboard/
        │   ├── security/
        │   │   ├── JwtValidatorTest.java                   # NUEVO
        │   │   └── JwtAuthInterceptorTest.java              # NUEVO
        │   └── dashboard/
        │       ├── controller/
        │       │   └── DashboardControllerTest.java         # @WebMvcTest — 3 endpoints, 200/401/400 por caso
        │       ├── service/
        │       │   ├── DashboardServiceTest.java
        │       │   ├── ChartsServiceTest.java                # NUEVO
        │       │   └── NewsServiceTest.java                  # NUEVO
        │       └── (adapter/cache/mock sin cambios de cobertura)
        └── resources/application.yml                          # + jwt.secret de test (≥32 bytes, fijo)
```

> Los DTOs internos de adapter (`DolarResponse`, `CryptoResponse`, `EquityQuoteResponse`, etc.) **no se tocan**:
> son el modelo interno de cada adapter. Los DTOs nuevos de `dto/` son el **contrato público** (lo que el
> controller devuelve); el service mapea de uno a otro. No usar MapStruct para estos mapeos (son agregaciones
> con claves dinámicas tipo `Map<String, X>`, no 1:1 entidad↔DTO); mapeo manual en el service está bien acá.

---

## 3. Modelo de datos del contrato público

### 3.1 `SummaryResponse` (`GET /api/v1/dashboard/summary`)

```java
public record SummaryResponse(
    String brand,                        // "Meridian" — nunca "Soma"/"Ops Deck"
    Instant generatedAt,
    FxSection fx,
    CryptoSection crypto,
    EquitiesSection equities,
    List<String> degraded               // subconjunto de {"fx", "crypto", "equities"} — nunca null, [] si todo ok
) {}

public record FxSection(Map<String, RateQuote> quotes) {}       // claves: "usd_oficial", "usd_blue"
public record RateQuote(BigDecimal compra, BigDecimal venta) {}

public record CryptoSection(Map<String, CryptoQuote> values) {} // claves: "bitcoin", "ethereum"
public record CryptoQuote(BigDecimal priceUsd, BigDecimal change24hPct) {}

public record EquitiesSection(Map<String, EquityQuote> values) {} // claves: "AAPL", "MSFT", "SPY"
public record EquityQuote(BigDecimal price, String currency) {}
```

**Mapeo de nombres (interno → público) — documentar explícitamente porque no son iguales:**

| Sección pública | Viene de (adapter interno) | Nota |
|---|---|---|
| `fx.quotes.usd_oficial` / `usd_blue` | `DolarApiAdapter` (tipo `oficial` / `blue`) | **el campo público `fx` son los tipos de dólar ARS**, no el Frankfurter internacional. Es la nomenclatura que exige H3; puede confundir porque H8 llama "fx" al adapter de Frankfurter — documentarlo en el README para que el próximo dev no lo reabra como bug |
| `crypto.bitcoin` / `crypto.ethereum` | `CoinGeckoAdapter.fetchPrices()` | `change24h` interno → `change24hPct` público (mismo valor, nombre más explícito) |
| `equities.AAPL` / `MSFT` / `SPY` | `FinnhubAdapter.fetchQuotes()` | sin cambio de valor, solo se indexa por ticker en vez de lista |
| `degraded` (summary) | `{dolar, crypto, equities}` internos → renombrar `"dolar"` a `"fx"` en la lista pública | Frankfurter y news **no** participan de `summary`; H3 menciona "news" en el enunciado general de degraded, pero `summary` no trae noticias — se documenta acá para no generar un valor `"news"` que no corresponde a esta respuesta |

### 3.2 `ChartsResponse` (`GET /api/v1/dashboard/charts`)

```java
public record ChartsResponse(String asset, String range, List<ChartPoint> points, boolean degraded) {}
public record ChartPoint(Instant t, BigDecimal v) {}
```

- `degraded = true` cuando el proveedor del activo solicitado falló/expiró sin cache válida (`points` puede
  venir vacío o con el último stale disponible, igual que hoy hace `DashboardCacheService`).
- Resuelve la pregunta abierta de H3 sobre "fallo parcial en charts": al ser una sola sección por request,
  un booleano es suficiente (no se necesita una lista como en `summary`).

### 3.3 `NewsResponse` (`GET /api/v1/dashboard/news`)

```java
public record NewsResponse(List<NewsArticleResponse> items, boolean degraded) {}
```

- Reutiliza `NewsArticleResponse` (ya tiene `id, title, summary, source, publishedAt, url, topic`).
- `degraded = true` **solo** cuando fallan NewsAPI **y** el fallback HN Algolia (el fallback a HN Algolia por
  sí solo no es degraded: es el comportamiento normal documentado en H8).

### 3.4 Activos y tópicos soportados

```java
public enum SupportedAsset {
    BTC("bitcoin", true), ETH("ethereum", true),
    AAPL("AAPL", false), MSFT("MSFT", false), SPY("SPY", false);
    // (symbol, isCrypto) — BTC/ETH resuelven contra CoinGeckoAdapter; AAPL/MSFT/SPY contra FinnhubAdapter
}
```

- `charts`: `asset` obligatorio, debe matchear un valor de `SupportedAsset` (case-insensitive aceptado,
  normalizado a mayúsculas); `range` opcional, default y único valor soportado `7d` (ver pregunta abierta de
  H3 — queda así hasta que se pida otro rango). Cualquier otro valor en `asset` o `range` → `UnsupportedParameterException`
  → 400 `VALIDATION_ERROR` con `FieldError(campo, "valor no soportado")`, **nunca invención de datos**.
- `news`: `topic` opcional, único valor soportado `ai-tech` (default si se omite); `limit` opcional, default
  `8`, rango válido `1..8` (`@Min(1) @Max(8)`); `topic` inválido → mismo `UnsupportedParameterException` → 400.
- **Frankfurter (FX internacional) queda fuera de los tres endpoints H3** por decisión de este documento: no
  hay ningún requisito H3 que lo use, y mantenerlo en la orquestación de `summary` generaría una llamada
  externa y una posible entrada en `degraded` para una sección que el contrato público ni siquiera expone.
  El adapter se conserva en el código (es válido para un futuro endpoint de FX internacional) pero
  `DashboardService.getSummary()` no lo invoca.

---

## 4. Endpoints expuestos (reemplazan al único `GET /api/v1/dashboard`)

Todos bajo JWT (ver §5). Ninguno modifica datos (solo lectura).

### 4.1 `GET /api/v1/dashboard/summary`

| | |
|---|---|
| Controller | `DashboardController.getSummary()` |
| Request | sin parámetros |
| 200 | `SummaryResponse` — siempre, mientras pueda servir al menos una sección (fresca, cacheada o stale) |
| 401 | sin JWT / JWT inválido o expirado → `ErrorCode.UNAUTHORIZED` |
| 500 | error inesperado de orquestación → `ErrorCode.INTERNAL_ERROR` |

### 4.2 `GET /api/v1/dashboard/charts?asset=<asset>&range=7d`

| | |
|---|---|
| Controller | `DashboardController.getCharts(asset, range)` |
| Request | `asset` obligatorio (`BTC\|ETH\|AAPL\|MSFT\|SPY`), `range` opcional (default `7d`, único valor soportado) |
| 200 | `ChartsResponse` |
| 400 | `asset` o `range` no soportado → `ErrorCode.VALIDATION_ERROR` (sin inventar datos) |
| 401 | sin JWT válido |
| 500 | error inesperado |

### 4.3 `GET /api/v1/dashboard/news?topic=ai-tech&limit=8`

| | |
|---|---|
| Controller | `DashboardController.getNews(topic, limit)` |
| Request | `topic` opcional (default/único valor `ai-tech`), `limit` opcional (default `8`, rango `1..8`) |
| 200 | `NewsResponse` — hasta `limit` artículos |
| 400 | `topic` no soportado o `limit` fuera de rango → `ErrorCode.VALIDATION_ERROR` |
| 401 | sin JWT válido |
| 500 | error inesperado |

---

## 5. Protección JWT (nueva, transversal a los tres endpoints)

### 5.1 Flujo

```
GET /api/v1/dashboard/{summary|charts|news}
  → JwtAuthInterceptor.preHandle()
      1. Lee header "Authorization"
      2. Si falta o no empieza con "Bearer " → UnauthorizedException → 401
      3. JwtValidator.isValid(token): verifica firma HS256 con JWT_SECRET y expiración (misma clave que
         auth-service; NO valida contra la base de usuarios — el dashboard-service no tiene persistencia)
      4. Si inválido/expirado → UnauthorizedException → 401
      5. Si válido → continúa al Controller
  → DashboardController.get*()  (solo se ejecuta si el interceptor dejó pasar la request)
```

- `JwtAuthInterceptor` se registra en `WebMvcConfig` **solo** para `/api/v1/dashboard/**`. No afecta
  `/actuator/health`, `/actuator/info`, `/v3/api-docs` ni `/swagger-ui.html` (deben seguir públicos).
- El dashboard-service no necesita el `userId`/`email` del token para responder (no hay datos por-usuario);
  `JwtValidator` solo confirma firma + expiración. No se agrega lectura de claims salvo que un endpoint
  futuro lo requiera.
- Mensaje de error 401: igual al de `auth-service` para consistencia de contrato entre servicios:
  `{"code":"UNAUTHORIZED","message":"Credenciales inválidas","details":[],"timestamp":"..."}`.

### 5.2 Config

`application.yml`:

```yaml
jwt:
  secret: ${JWT_SECRET}
```

- Sin `expiration-ms`: el dashboard-service no emite tokens, solo valida el `exp` que ya viene en el JWT.
- `JWT_SECRET` es **obligatoria** (sin default) — el servicio no debe arrancar sin ella (mismo criterio que
  `auth-service`, que ya hace `Objects.requireNonNull` + validación de longitud ≥ 32 bytes en `JwtConfig`).
  Esto es intencional y distinto del resto de variables del dashboard (`FINNHUB_API_KEY`, `NEWS_API_KEY`,
  que sí son opcionales): sin secreto no hay forma de validar JWT, y servir datos sin poder autenticar
  violaría el requisito "Protección por JWT en todos los endpoints del dashboard".

---

## 6. OpenAPI

- Agregar `@SecurityScheme(name = "bearerAuth", type = HTTP, scheme = "bearer", bearerFormat = "JWT")` a
  nivel de `DashboardController` (igual que `AuthController`) y `@SecurityRequirement(name = "bearerAuth")`
  en los tres métodos.
- Reemplazar el ejemplo `DashboardMock` (shape viejo) por tres ejemplos nuevos: `SummaryMock`, `ChartsMock`,
  `NewsMock`, usando `DashboardMockFixtures` adaptado a los nuevos DTOs.
- Cumple el requisito H3 "OpenAPI actualizado": los tres paths documentados con parámetros, shape de
  respuesta y el esquema de seguridad JWT.

---

## 7. Cache y degraded (extiende §6 de H8, sin cambiarla)

- `DashboardCacheService` no cambia de implementación. Cambia **qué** se cachea: ya no hay una key
  `"dolar"`/`"crypto"`/`"equities"`/`"news"` compartida por un único endpoint — las mismas keys se reutilizan
  ahora desde `DashboardService` (summary), `ChartsService` (una key por asset, ej. `"chart:BTC:7d"`) y
  `NewsService` (`"news:ai-tech"`). TTL sigue siendo `DASHBOARD_CACHE_TTL_SECONDS` (default 60s).
- `summary.degraded`: lista, como hoy (ver mapeo de nombres en §3.1).
- `charts.degraded` / `news.degraded`: booleano (ver §3.2–§3.3). Mismo criterio de "frescos vs stale vs sin
  dato" ya implementado en `CacheResult`.

---

## 8. `pom.xml` — cambios

| Acción | Dependencia |
|--------|-------------|
| Agregar | `io.jsonwebtoken:jjwt-api`, `jjwt-impl` (scope runtime), `jjwt-jackson` (scope runtime) — misma versión que `auth-service` (`jjwt.version` en `<properties>`, hoy `0.12.6`) |
| Mantener | todo lo de H8 (`spring-boot-starter-webflux`, `mockwebserver` en test, etc.) |
| No agregar | `spring-boot-starter-security` (se valida con JJWT a mano, igual que `auth-service`; ver §1) |

---

## 9. Tests (extiende §9 de H8)

| Capa | Clase | Qué prueba |
|------|-------|------------|
| Security | `JwtValidatorTest` | token válido / expirado / firma distinta / malformado |
| Security | `JwtAuthInterceptorTest` | sin header → 401; header sin "Bearer " → 401; token válido → deja pasar |
| Controller | `DashboardControllerTest` | `summary`/`charts`/`news`: 200 con JWT válido (mock mode), 401 sin JWT, 400 en `asset`/`range`/`topic`/`limit` inválidos |
| Service | `DashboardServiceTest` | ya no llama a `FrankfurterAdapter`; `degraded` solo con `{fx, crypto, equities}` |
| Service | `ChartsServiceTest` (nuevo) | BTC/ETH vía CoinGecko, equity vía Finnhub candles, asset no soportado → excepción, degraded cuando el proveedor falla |
| Service | `NewsServiceTest` (nuevo) | NewsAPI ok (no degraded); NewsAPI caído + fallback HN ok (no degraded); ambos caídos → degraded=true |
| Integración | `DashboardApplicationTest` | sin cambios (contextLoads, `DASHBOARD_USE_MOCK=true`) |

JaCoCo sigue en 80% mínimo. `src/test/resources/application.yml` necesita un `jwt.secret` fijo (≥32 bytes,
cualquier valor de test) para que el contexto levante.

---

## 10. Tareas de DevOps / Documentación derivadas (no son trabajo del arquitecto, se dejan explícitas para Tech Lead)

1. **Compartir `JWT_SECRET`** entre `auth-service` y `dashboard-service` en `docker-compose.yml`, `.env`/`.env.example`, CI (GitHub Actions) y Helm — mismo valor, una sola variable de entorno gestionada como secreto en cada entorno.
2. Actualizar `README.md`, Postman/Bruno y OpenAPI exportado del dashboard-service con los tres endpoints nuevos y el requisito de `Authorization: Bearer <token>` (ej. flujo: login contra `auth-service` → usar el `accessToken` contra `dashboard-service`).
3. Nada que hacer sobre `.m2-local`: confirmado removido del repo y cubierto por `.gitignore`/`.dockerignore` (ver §0).

---

## 11. Checklist de arquitectura

- [ ] Tres endpoints (`summary`, `charts`, `news`) protegidos por JWT compartido con `auth-service`, bajo `/api/v1/dashboard/`
- [ ] `DashboardResponse` plano y `GET /api/v1/dashboard` sin JWT quedan retirados (no conviven con el nuevo contrato)
- [ ] `SummaryResponse` con `brand`, `generatedAt`, `fx.quotes.{usd_oficial,usd_blue}`, `crypto.{bitcoin,ethereum}`, `equities.{AAPL,MSFT,SPY}`, `degraded: []`
- [ ] `ChartsResponse`/`NewsResponse` con `degraded` booleano (decisión de este documento para la pregunta abierta de H3)
- [ ] Tercer ticker de equities confirmado: `SPY`
- [ ] Frankfurter implementado pero no invocado desde los endpoints públicos H3
- [ ] Sin `spring-boot-starter-security`; validación JWT manual con JJWT, igual patrón que `auth-service`
- [ ] `JWT_SECRET` obligatorio (sin default), mismo valor que `auth-service`
- [ ] OpenAPI con `bearerAuth` en los tres endpoints
- [ ] Ningún campo `activeUsers`/`sessionsToday`/`errors` ni mención a `Soma`/`SOMA`/`Ops Deck` en ningún DTO público (verificado: no hay ocurrencias en el código actual)
- [ ] `.m2-local` confirmado fuera del repositorio; sin acción pendiente
- [ ] JaCoCo ≥ 80%; `./mvnw -q -B verify` pasa
