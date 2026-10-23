# Contratos OpenAPI versionados

El repositorio tiene más de un servicio y cada build exporta su propio contrato. Para no pisar el
archivo de otro servicio, el layout es:

| Archivo | Servicio | Lo genera |
|---|---|---|
| `openapi.json` / `openapi.yaml` | `auth-service` | `services/auth-service/src/test/java/com/meridian/users/docs/OpenApiExportTest.java` |
| `dashboard-service/openapi.json` / `.yaml` | `dashboard-service` | `services/dashboard-service/src/test/java/com/meridian/dashboard/docs/OpenApiExportTest.java` |

Cada `OpenApiExportTest` corre en `verify` y escribe en su ruta: el contrato versionado no se edita a
mano y siempre coincide con el código.

## Regenerar

```powershell
Set-Location services/auth-service
.\mvnw.cmd -q -B verify

Set-Location ../dashboard-service
.\mvnw.cmd -q -B verify
```

En Linux/macOS usar `./mvnw`. El build de un servicio no modifica el contrato del otro.

## Consultar en ejecución

| Servicio | Swagger UI | OpenAPI |
|---|---|---|
| `auth-service` | http://localhost:8080/swagger-ui.html | http://localhost:8080/v3/api-docs |
| `dashboard-service` | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs y `/v3/api-docs/dashboard` |

Los ejemplos de las colecciones (Postman y Bruno) y de `delivery/api.md` salen de estos contratos y de
respuestas reales del servicio; no se inventan endpoints ni campos.
