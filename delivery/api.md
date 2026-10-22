# auth-service — uso de la API con curl

Base URL local: `http://localhost:8080`. Todos los endpoints están bajo `/api/v1` (el versionado
de API está deshabilitado en `project.yaml`, por lo que existe una sola versión).

Los ejemplos usan el mismo usuario que Swagger, Postman y Bruno:

| Campo | Valor |
|---|---|
| id | `6710f3a2c9e77c0012345678` |
| name | `Ana Pérez` |
| email | `ana.perez@example.com` |
| password | `password123` (mínimo 8 caracteres) |
| createdAt | `2026-10-22T10:00:00Z` |

> En Windows, `curl` es un alias de `Invoke-WebRequest` en PowerShell 5.1. Usar `curl.exe` para los
> comandos de abajo. En Linux/macOS usar `curl` normalmente.

## Convenciones

- Request y response: `Content-Type: application/json`.
- Fechas: ISO-8601 en UTC (`2026-10-22T10:00:00Z`).
- La contraseña viaja en texto plano solo en el alta; el servicio la guarda como hash BCrypt.
- El email se normaliza (trim y minúsculas) antes de persistirse.
- Si se repite el mismo email, la API responde `409 CONFLICT`; para volver a probar el alta usar un
  email distinto.

## POST /api/v1/users — registrar usuario

```bash
curl -i -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Ana Pérez",
    "email": "ana.perez@example.com",
    "password": "password123"
  }'
```

Respuesta `201 Created`:

```json
{
  "id": "6710f3a2c9e77c0012345678",
  "name": "Ana Pérez",
  "email": "ana.perez@example.com",
  "createdAt": "2026-10-22T10:00:00Z"
}
```

La respuesta no incluye `password` ni `passwordHash`.

### Errores posibles

`400 VALIDATION_ERROR` — email con formato inválido (o campo obligatorio ausente / contraseña de
menos de 8 caracteres):

```bash
curl -i -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Ana Pérez","email":"invalido","password":"password123"}'
```

```json
{
  "code": "VALIDATION_ERROR",
  "message": "La solicitud tiene datos inválidos",
  "details": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

`400 BAD_REQUEST` — JSON mal formado:

```bash
curl -i -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Ana Pérez",'
```

```json
{
  "code": "BAD_REQUEST",
  "message": "Solicitud inválida",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

`409 CONFLICT` — el email ya está registrado:

```bash
curl -i -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name":"Otra Ana","email":"ana.perez@example.com","password":"password123"}'
```

```json
{
  "code": "CONFLICT",
  "message": "Email ya registrado",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

`500 INTERNAL_ERROR` — error inesperado (no se provoca desde un cliente sano):

```json
{
  "code": "INTERNAL_ERROR",
  "message": "Error interno",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

## GET /api/v1/users/{id} — consultar usuario

Usar el `id` devuelto por el alta:

```bash
curl -i http://localhost:8080/api/v1/users/6710f3a2c9e77c0012345678
```

Respuesta `200 OK`:

```json
{
  "id": "6710f3a2c9e77c0012345678",
  "name": "Ana Pérez",
  "email": "ana.perez@example.com",
  "createdAt": "2026-10-22T10:00:00Z"
}
```

### Errores posibles

`404 NOT_FOUND` — no existe un usuario con ese id:

```bash
curl -i http://localhost:8080/api/v1/users/6710f3a2c9e77c00ffffffff
```

```json
{
  "code": "NOT_FOUND",
  "message": "Usuario no encontrado por id: 6710f3a2c9e77c00ffffffff",
  "details": [],
  "timestamp": "2026-10-22T10:05:00Z"
}
```

`500 INTERNAL_ERROR` — error inesperado, con el mismo JSON genérico del alta.

## GET /actuator/health — estado del servicio

```bash
curl -i http://localhost:8080/actuator/health
```

Respuesta `200 OK`:

```json
{
  "status": "UP"
}
```

## Referencias

- Especificación completa: `delivery/openapi/openapi.json` y `delivery/openapi/openapi.yaml`.
- Swagger UI: http://localhost:8080/swagger-ui.html
- Colecciones: `delivery/postman/` y `delivery/bruno/auth-service/`.
- Contrato de errores: `src/main/java/com/meridian/users/exception/GlobalExceptionHandler.java`.
