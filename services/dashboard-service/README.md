# dashboard-service

Base del servicio de APIs públicas de Meridian (Java 21, puerto 8081).

Desde esta carpeta: `./mvnw clean verify` para compilar y probar, y
`./mvnw spring-boot:run` para ejecutar (Windows: `mvnw.cmd`).

Configuración opcional: `PORT` (8081), `DASHBOARD_USE_MOCK` (false),
`DASHBOARD_CACHE_TTL_SECONDS` (60), `FINNHUB_API_KEY` y `NEWS_API_KEY` (vacías).
Cada proveedor acepta `DASHBOARD_<PROVEEDOR>_BASE_URL` y
`DASHBOARD_<PROVEEDOR>_TIMEOUT_SECONDS` (5); los nombres están en application.yml.
Las claves se consumirán en los adapters; no se incorporan a los clientes base.

Actuator: `/actuator/health`, `/actuator/info`. OpenAPI: `/v3/api-docs/dashboard`;
Swagger UI: `/swagger-ui.html`. El endpoint se implementa en una tarea posterior.

Los adapters DolarApi y Frankfurter consultan dólar oficial/blue y USD→EUR/BRL,
respectivamente, con un límite total de cinco segundos y propagación de errores
al agregador. La caché TTL y la gestión de secciones degraded corresponden a otras tareas.
Con `DASHBOARD_USE_MOCK=true` devuelven fixtures sin llamadas HTTP: dólar oficial
1000/1040 ARS (compra/venta), blue 1200/1220 ARS y tasas USD→EUR 0.92,
USD→BRL 5.10. Son datos ficticios estáticos, no cotizaciones actuales.
