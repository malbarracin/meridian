---
title: Dashboard — Especificación funcional (H3)
status: borrador
source: https://app.notion.com/p/3ab75bb08ca7810faa9ddb10777255f7
updated: 2026-10-23T23:56:21.576Z
---

# Dashboard — Especificación funcional (H3)

## Propósito

Construir la API del dashboard de **Meridian** (`services/dashboard-service`): endpoints protegidos por JWT
que agregan datos de **dólar, bolsa, cripto y noticias IA/tecnología** desde APIs públicas, para alimentar
la pantalla de mercados de Meridian (web/mobile). Esta especificación cubre el **contrato de los endpoints**
expuestos al cliente. El catálogo de proveedores externos y sus adapters está en `dashboard/apis-publicas`
(H8); esta spec define **qué expone el dashboard-service hacia afuera** y bajo qué reglas (autenticación,
forma de la respuesta, errores).

No reemplaza `dashboard/apis-publicas`: la consume. Si hay una discrepancia entre el modelo de datos interno
descripto en H8 y el shape de respuesta exigido acá, **este documento (H3) es el contrato público** que debe
cumplirse; los adapters internos se ajustan a él.

## Requisitos

### Requisito: Resumen del dashboard (`summary`)

El sistema DEBE exponer `GET /api/v1/dashboard/summary`, protegido por JWT, que devuelve en una sola
respuesta las cotizaciones de dólar (`fx`), precios de cripto (`crypto`) y cotizaciones de bolsa (`equities`)
necesarias para la pantalla principal del dashboard.

#### Escenario: Resumen exitoso con todas las fuentes disponibles

- **DADO** que el cliente envía un JWT válido y todos los proveedores externos responden
- **CUANDO** se invoca `GET /api/v1/dashboard/summary`
- **ENTONCES** el sistema responde HTTP 200 OK
- **Y** el cuerpo incluye `brand`, `generatedAt`, `fx.quotes` (con al menos `usd_oficial` y `usd_blue`),
  `crypto` (con al menos `bitcoin` y `ethereum`, cada uno con `priceUsd` y `change24hPct`),
  `equities` (con al menos `AAPL`, `MSFT` y un índice/proxy de mercado) y `degraded` como lista vacía

#### Escenario: Resumen con una fuente caída (degradado parcial)

- **DADO** que el cliente envía un JWT válido y uno de los proveedores (fx, crypto o equities) falla o no
  responde dentro del timeout
- **CUANDO** se invoca `GET /api/v1/dashboard/summary`
- **ENTONCES** el sistema responde HTTP 200 OK (no 5xx) con los datos de las secciones disponibles
- **Y** `degraded` incluye el nombre de la sección afectada (`fx` | `crypto` | `equities` | `news`)

#### Escenario: Sin JWT

- **DADO** que el cliente no envía `Authorization: Bearer <token>`, o el token es inválido o expiró
- **CUANDO** se invoca `GET /api/v1/dashboard/summary`
- **ENTONCES** el sistema responde HTTP 401 Unauthorized y no incluye ningún dato de mercado en el cuerpo

---

### Requisito: Series históricas para gráficos (`charts`)

El sistema DEBE exponer `GET /api/v1/dashboard/charts?asset=<asset>&range=<range>`, protegido por JWT,
que devuelve la serie de puntos (timestamp + valor) de un activo para graficar su evolución. DEBE soportar
como mínimo los activos `BTC`, `ETH` y un equity/índice de bolsa (ej. `AAPL`) con rango `7d`.

#### Escenario: Serie histórica de cripto

- **DADO** que el cliente envía un JWT válido y `asset=BTC&range=7d`
- **CUANDO** se invoca `GET /api/v1/dashboard/charts`
- **ENTONCES** el sistema responde HTTP 200 OK con `asset`, `range` y `points` (lista de `{t, v}`)
  cubriendo los últimos 7 días

#### Escenario: Serie histórica de un equity/índice

- **DADO** que el cliente envía un JWT válido y `asset=<equity soportado>&range=7d`
- **CUANDO** se invoca `GET /api/v1/dashboard/charts`
- **ENTONCES** el sistema responde HTTP 200 OK con `points` usables para graficar la evolución del activo

#### Escenario: Activo no soportado

- **DADO** que el cliente envía un `asset` fuera del catálogo soportado (cripto o equity/índice mínimo)
- **CUANDO** se invoca `GET /api/v1/dashboard/charts`
- **ENTONCES** el sistema responde con un error del contrato de error estándar del servicio (sin inventar datos)

#### Escenario: Sin JWT

- **DADO** que el cliente no envía un JWT válido
- **CUANDO** se invoca `GET /api/v1/dashboard/charts`
- **ENTONCES** el sistema responde HTTP 401 Unauthorized

---

### Requisito: Noticias IA / tecnología (`news`)

El sistema DEBE exponer `GET /api/v1/dashboard/news?topic=ai-tech&limit=8`, protegido por JWT, que devuelve
hasta `limit` noticias normalizadas filtradas por el tópico `ai-tech`.

#### Escenario: Noticias disponibles

- **DADO** que el cliente envía un JWT válido
- **CUANDO** se invoca `GET /api/v1/dashboard/news?topic=ai-tech&limit=8`
- **ENTONCES** el sistema responde HTTP 200 OK con `items`: hasta 8 artículos, cada uno con `id`, `title`,
  `summary`, `source`, `publishedAt`, `url` y `topic=ai-tech`

#### Escenario: Sin JWT

- **DADO** que el cliente no envía un JWT válido
- **CUANDO** se invoca `GET /api/v1/dashboard/news`
- **ENTONCES** el sistema responde HTTP 401 Unauthorized

---

### Requisito: Protección por JWT en todos los endpoints del dashboard

El sistema DEBE exigir un JWT válido (emitido por el área auth, H2) en **todos** los endpoints del
dashboard. Sin JWT válido, el sistema NO DEBE devolver ningún dato de mercado ni de noticias.

#### Escenario: Token expirado

- **DADO** que el cliente envía un JWT expirado
- **CUANDO** se invoca cualquier endpoint de `/api/v1/dashboard/*`
- **ENTONCES** el sistema responde HTTP 401 Unauthorized

---

### Requisito: Las API keys de proveedores nunca llegan al cliente

El sistema DEBE mantener las API keys de proveedores externos (`FINNHUB_API_KEY`, `NEWS_API_KEY`, etc.)
exclusivamente en el backend. El frontend (web/mobile) NO DEBE llamar directamente a CoinGecko, Finnhub,
NewsAPI ni ningún proveedor externo: siempre pasa por `dashboard-service`.

#### Escenario: Respuesta del dashboard sin credenciales de proveedores

- **DADO** cualquier respuesta exitosa o degradada de los endpoints del dashboard
- **CUANDO** el cliente recibe el cuerpo de la respuesta
- **ENTONCES** no contiene ninguna API key, token de proveedor externo ni URL con credenciales embebidas

---

### Requisito: Contenido prohibido (branding y KPIs operativos)

El sistema NO DEBE devolver como contenido principal el shape de KPIs operativos genéricos
(`activeUsers`, `sessionsToday`, `errors`) ni ninguna referencia a la marca `Soma` / `SOMA` / `Ops Deck`
en los datos de respuesta.

#### Escenario: Respuesta libre de branding y KPIs de ops

- **DADO** cualquier respuesta de `summary`, `charts` o `news`
- **CUANDO** se inspecciona el cuerpo
- **ENTONCES** no aparecen los campos `activeUsers`, `sessionsToday`, `errors` como contenido principal
- **Y** no aparece ninguna mención a `Soma`, `SOMA`, `SOMA / OPS` ni `Ops Deck`

---

### Requisito: OpenAPI actualizado con los contratos del dashboard

El sistema DEBE publicar la documentación OpenAPI del `dashboard-service` reflejando los tres endpoints
(`summary`, `charts`, `news`) con sus parámetros, shapes de respuesta y el requisito de JWT.

#### Escenario: OpenAPI accesible y completo

- **DADO** que el `dashboard-service` está corriendo
- **CUANDO** se accede a la URL de OpenAPI (ej. `/v3/api-docs` o `/swagger-ui`)
- **ENTONCES** el sistema devuelve la especificación con `GET /api/v1/dashboard/summary`,
  `GET /api/v1/dashboard/charts` y `GET /api/v1/dashboard/news` documentados, incluyendo el esquema de
  seguridad JWT

---

## Fuera de alcance (H3)

- Trading / órdenes de compra-venta
- Analytics internos de "usuarios activos / sesiones / errores" como producto del dashboard
- Todo lo fuera de alcance ya definido en `auth/spec` (H2) para la emisión/validación de JWT

## Dependencias

| Hito | Relación |
|------|----------|
| H2 (`auth/spec`) | El dashboard-service valida los JWT emitidos por el área auth |
| H1 | Define el contenido visual que consumen estos contratos |
| H8 (`dashboard/apis-publicas`) | Catálogo de proveedores y adapters que alimentan estos endpoints |
| Desbloquea | H5, H7 |

## Preguntas abiertas

- **Representación de fallo parcial en `charts` y `news`**: el shape de ejemplo de H3 solo muestra el campo
  `degraded` dentro de `summary`. No especifica qué devuelven `charts` o `news` cuando su proveedor falla
  (¿`points`/`items` vacío con 200, un campo `degraded` propio, u otra señal?). El arquitecto define el
  contrato exacto siguiendo el principio general de H3 ("200 con degradado preferido sobre 5xx total").
- **Tercer ticker de equities/índice**: el shape de ejemplo de H3 usa `AAPL`, `MSFT` y `^GSPC` (S&P 500);
  `dashboard/apis-publicas` (H8) propone `AAPL`, `MSFT` y `SPY` como proxy. `^GSPC` es un símbolo de índice
  que puede no estar disponible en el free tier de Finnhub. El arquitecto/dev confirma el ticker exacto al
  verificar disponibilidad y lo documenta (coherente con la pregunta abierta ya registrada en H8).
- **Conciliación con el borrador de arquitectura existente**: `dashboard/arquitectura-apis-publicas.md`
  (borrador, previo a esta especificación) propone un único endpoint `GET /api/v1/dashboard` sin protección
  JWT. Este documento (H3) exige tres endpoints (`summary`, `charts`, `news`) protegidos por JWT. El
  arquitecto DEBE revisar y actualizar ese borrador para alinearlo con este contrato antes de pasar a
  desarrollo.
- **Rangos soportados en `charts`**: la fuente solo ejemplifica `range=7d`. No se define si hay otros rangos
  obligatorios (ej. `24h`, `30d`). Se asume que `7d` es el único rango exigido por H3; cualquier rango
  adicional es una extensión, no un requisito de este hito.
