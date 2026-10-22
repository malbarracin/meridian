---
title: Dashboard — Catálogo de APIs públicas (H8)
status: borrador
source: https://app.notion.com/p/3ac75bb08ca781f5b410e46a6ec7143d
updated: 2026-10-22T23:06:59.215Z
---

# Dashboard — Catálogo de APIs públicas (H8)

## Propósito

Definir los proveedores externos exactos que el **Dashboard Service** de Meridian debe integrar vía adapters
para servir cotizaciones de dólar/FX, precios de criptomonedas, datos bursátiles y noticias de IA/tecnología.
El cliente (web/mobile) **solo** habla con `dashboard-service`; las API keys nunca se exponen al frontend.

## Principios transversales

- Timeout corto (3–5 s) y retry limitado en todos los adapters.
- Cache con TTL configurable (variable `DASHBOARD_CACHE_TTL_SECONDS`, mínimo 60 s para CoinGecko).
- Si un proveedor falla → marcar la sección correspondiente como `degraded` y servir cache o mock.
- `DASHBOARD_USE_MOCK=true` habilita modo demo offline (fixtures estáticas para todos los adapters).
- Las API keys se leen exclusivamente de variables de entorno del backend.

---

## Requisitos

### Requisito: Adapter Dólar / FX (DolarApi)

El sistema DEBE integrar **DolarApi** para obtener cotizaciones del dólar en pesos argentinos (ARS),
mapeando al menos los tipos oficial y blue.

#### Escenario: Consulta exitosa de cotizaciones de dólar

- **DADO** que el adapter DolarApi está configurado y el proveedor responde
- **CUANDO** el dashboard-service solicita cotizaciones de dólar
- **ENTONCES** devuelve al menos los tipos `oficial` y `blue` mapeados al modelo interno
- **Y** los datos quedan cacheados con el TTL configurado

#### Escenario: DolarApi no disponible

- **DADO** que DolarApi no responde dentro del timeout
- **CUANDO** el dashboard-service solicita cotizaciones de dólar
- **ENTONCES** la sección de dólar se marca `degraded`
- **Y** se sirve la última respuesta cacheada o el mock si `DASHBOARD_USE_MOCK=true`

---

### Requisito: Adapter FX internacional (Frankfurter — opcional)

El sistema PUEDE integrar **Frankfurter** (ECB) para cotizaciones de divisas internacionales (ej. USD→EUR, USD→BRL).

#### Escenario: Consulta exitosa de tipo de cambio internacional

- **DADO** que el adapter Frankfurter está habilitado
- **CUANDO** se solicitan tipos de cambio internacionales
- **ENTONCES** devuelve las divisas configuradas (ej. EUR, BRL) en el modelo interno
- **Y** los datos quedan cacheados con el TTL configurado

#### Escenario: Frankfurter no disponible

- **DADO** que Frankfurter no responde dentro del timeout
- **CUANDO** se solicitan tipos de cambio internacionales
- **ENTONCES** la sección FX internacional se marca `degraded` y se sirve cache o mock

---

### Requisito: Adapter Cripto — precio actual (CoinGecko)

El sistema DEBE integrar **CoinGecko** para obtener precio y variación 24h de Bitcoin y Ethereum en USD.

#### Escenario: Consulta exitosa de precio cripto

- **DADO** que el adapter CoinGecko está configurado
- **CUANDO** se solicita el precio de BTC y ETH
- **ENTONCES** devuelve precio actual y variación 24h para `bitcoin` y `ethereum` en USD
- **Y** los datos se cachean con TTL ≥ 60 s (respetando el rate limit de CoinGecko)

#### Escenario: CoinGecko no disponible

- **DADO** que CoinGecko no responde dentro del timeout
- **CUANDO** se solicita precio de cripto
- **ENTONCES** la sección cripto se marca `degraded` y se sirve cache o mock

---

### Requisito: Adapter Cripto — serie histórica 7d (CoinGecko)

El sistema DEBE integrar **CoinGecko** para obtener la serie de precios de los últimos 7 días de BTC y ETH
(para uso en gráficos).

#### Escenario: Consulta exitosa de serie histórica

- **DADO** que el adapter CoinGecko está configurado
- **CUANDO** se solicita la serie de mercado de 7 días para `bitcoin` o `ethereum`
- **ENTONCES** devuelve los puntos de precio en USD necesarios para renderizar el gráfico

#### Escenario: CoinGecko market_chart no disponible

- **DADO** que CoinGecko no responde dentro del timeout
- **CUANDO** se solicita la serie histórica
- **ENTONCES** la sección de gráfico cripto se marca `degraded` y se sirve cache o mock

---

### Requisito: Adapter Bolsa / Equities — quote (Finnhub)

El sistema DEBE integrar **Finnhub** para obtener cotizaciones de acciones e índices, con los tickers
mínimos `AAPL`, `MSFT` y un proxy de mercado (`SPY` o equivalente disponible en free tier).
La API key se lee de `FINNHUB_API_KEY`.

#### Escenario: Consulta exitosa de quote de acciones

- **DADO** que `FINNHUB_API_KEY` está configurada y Finnhub responde
- **CUANDO** se solicitan quotes de los tickers mínimos
- **ENTONCES** devuelve el precio actual para cada ticker mapeado al modelo interno

#### Escenario: Finnhub sin API key (modo demo)

- **DADO** que `FINNHUB_API_KEY` no está configurada o `DASHBOARD_USE_MOCK=true`
- **CUANDO** se solicitan quotes de equities
- **ENTONCES** el adapter sirve fixtures de equities documentadas (sin inventar precios dinámicos)

#### Escenario: Finnhub no disponible

- **DADO** que Finnhub no responde dentro del timeout
- **CUANDO** se solicitan quotes de equities
- **ENTONCES** la sección de bolsa se marca `degraded` y se sirve cache o mock/fixtures

---

### Requisito: Adapter Bolsa / Equities — velas históricas (Finnhub)

El sistema DEBE integrar **Finnhub** para obtener datos de velas (OHLC) diarias necesarios para gráficos,
usando los mismos tickers que el quote.

#### Escenario: Consulta exitosa de velas

- **DADO** que `FINNHUB_API_KEY` está configurada y Finnhub responde
- **CUANDO** se solicitan velas históricas para un ticker
- **ENTONCES** devuelve los datos OHLC en el rango solicitado mapeados al modelo interno

#### Escenario: Finnhub candle no disponible

- **DADO** que Finnhub no responde
- **CUANDO** se solicitan velas
- **ENTONCES** la sección de gráfico de bolsa se marca `degraded` y se sirve cache o mock

---

### Requisito: Adapter Noticias IA/Tech — feed principal (NewsAPI)

El sistema DEBE integrar **NewsAPI** para obtener las últimas noticias sobre IA y tecnología
(idioma inglés, ordenadas por fecha de publicación, máximo 8 resultados).
La API key se lee de `NEWS_API_KEY`.

#### Escenario: Consulta exitosa de noticias con NewsAPI

- **DADO** que `NEWS_API_KEY` está configurada y NewsAPI responde
- **CUANDO** se solicita el feed de noticias AI/Tech
- **ENTONCES** devuelve hasta 8 artículos normalizados con los campos:
  `id`, `title`, `summary`, `source`, `publishedAt`, `url`, `topic=ai-tech`

#### Escenario: NewsAPI sin API key o no disponible

- **DADO** que `NEWS_API_KEY` no está configurada o NewsAPI no responde
- **CUANDO** se solicita el feed de noticias
- **ENTONCES** el adapter usa el fallback HN Algolia automáticamente

---

### Requisito: Adapter Noticias IA/Tech — fallback HN Algolia

El sistema DEBE integrar **HN Algolia** como fallback de noticias cuando NewsAPI no está disponible.
No requiere API key.

#### Escenario: Fallback a HN Algolia

- **DADO** que NewsAPI no responde o no tiene key configurada
- **CUANDO** el adapter de noticias solicita el fallback
- **ENTONCES** obtiene historias de HN Algolia con query `AI technology`
- **Y** mapea `title`, `url`, `created_at` al modelo normalizado (`id`, `title`, `summary`, `source`, `publishedAt`, `url`, `topic=ai-tech`)

#### Escenario: HN Algolia no disponible

- **DADO** que tanto NewsAPI como HN Algolia no responden
- **CUANDO** se solicita el feed de noticias
- **ENTONCES** la sección de noticias se marca `degraded` y se sirve cache o mock

---

### Requisito: Cache y modo degraded

El sistema DEBE implementar cache con TTL y comportamiento degraded para todos los adapters externos.

#### Escenario: Respuesta cacheada dentro del TTL

- **DADO** que una consulta anterior fue exitosa y el TTL no expiró
- **CUANDO** se solicitan los mismos datos
- **ENTONCES** se sirve la respuesta cacheada sin llamar al proveedor externo

#### Escenario: Proveedor caído con cache vencida

- **DADO** que el TTL expiró y el proveedor no responde
- **CUANDO** se solicitan los datos
- **ENTONCES** la sección correspondiente se marca `degraded` en la respuesta del dashboard
- **Y** si hay datos cacheados aunque vencidos, se sirven con indicador `stale`

---

### Requisito: Modo demo offline (DASHBOARD_USE_MOCK)

El sistema DEBE soportar un modo demo offline donde todos los adapters sirven fixtures estáticas
sin llamadas reales a proveedores externos.

#### Escenario: Modo mock habilitado

- **DADO** que `DASHBOARD_USE_MOCK=true` en el entorno
- **CUANDO** se consulta cualquier dato del dashboard
- **ENTONCES** todos los adapters devuelven fixtures estáticas documentadas
- **Y** no se realiza ninguna llamada HTTP a proveedores externos

---

### Requisito: Configuración por variables de entorno

El sistema DEBE leer todas las API keys y parámetros de configuración exclusivamente de variables de entorno.
Las keys nunca deben aparecer en el frontend ni en código fuente.

#### Escenario: Servicio arranca sin keys opcionales

- **DADO** que `FINNHUB_API_KEY` o `NEWS_API_KEY` no están configuradas
- **CUANDO** arranca el dashboard-service
- **ENTONCES** el servicio arranca sin error, usando mock/fallback para los adapters sin key

#### Escenario: Variables mínimas requeridas

Las variables de entorno del servicio son:
- `FINNHUB_API_KEY` — key Finnhub (opcional; sin ella: fixtures de equities)
- `NEWS_API_KEY` — key NewsAPI (opcional; sin ella: fallback a HN Algolia)
- `DASHBOARD_CACHE_TTL_SECONDS` — TTL de cache en segundos (por defecto: 60)
- `DASHBOARD_USE_MOCK` — habilita modo demo offline (por defecto: false)

---

## Dependencias de hitos

| Hito | Relación |
|------|----------|
| H3 | Este hito implementa los contratos definidos en H3 |
| H1 | Informado por H1 (qué mostrar en el dashboard) |
| H5, H7 | Consumido por H5 y H7 |

## Preguntas abiertas

- El hito menciona `SPY` o "quote de índice según disponibilidad free" como proxy de mercado para bolsa: la selección exacta del tercer ticker queda pendiente de verificar disponibilidad en el free tier de Finnhub al momento de implementar.
- El modelo normalizado de datos (contratos internos entre adapters y el servicio agregador) lo define H3; esta spec no lo detalla para no duplicar.
