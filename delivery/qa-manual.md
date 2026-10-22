# auth-service — manual de QA

Guía para validar el servicio con las colecciones de Postman y Bruno. Todos los pedidos apuntan a
los endpoints reales del código y usan los mismos ejemplos que Swagger y `delivery/api.md`.

## Antes de empezar

1. MongoDB 7 disponible y el servicio arrancado con las variables obligatorias:

   ```powershell
   Set-Location services/auth-service
   $env:MONGO_URI = 'mongodb://localhost:27017'
   $env:MONGO_DATABASE = 'auth_local'
   $env:JWT_SECRET = 'dev-only-jwt-secret-change-me-32-chars-minimum'
   $env:JWT_EXPIRATION_MS = '86400000'
   $env:SPRING_PROFILES_ACTIVE = 'dev'
   .\mvnw.cmd spring-boot:run
   ```

   `JWT_SECRET` es obligatoria (mínimo 32 bytes); en local se puede usar la clave pública de
   `.env.example`. `JWT_EXPIRATION_MS` es opcional (24 horas por defecto). No usar valores reales de
   otros entornos.

2. Verificar que responde:

   ```powershell
   Invoke-WebRequest http://localhost:8080/actuator/health
   ```

   Debe devolver `200` con `{"status":"UP"}`. Si el arranque falla, revisar que `MONGO_URI`,
   `MONGO_DATABASE` y `JWT_SECRET` estén definidas y que MongoDB sea accesible.

3. La colección usa `baseUrl = http://localhost:8080`. Si el servicio corre en otro puerto, cambiar
   la variable `baseUrl` en el entorno.

Docker no es necesario para probar la API con Postman o Bruno: solo hace falta para la suite
automatizada con Testcontainers (ver `delivery/testing.md`).

## Postman

### Importar

1. Abrir Postman → **Import**.
2. Importar `delivery/postman/auth-service.postman_collection.json`.
3. Importar `delivery/postman/auth-service.postman_environment.json`.
4. Seleccionar el entorno **auth-service - local** en el selector superior derecho.

La colección trae variables propias (`baseUrl`, `userId`, `userEmail`, `authToken`). El pedido de
alta genera un email único por corrida, guarda el `id` en `userId` y el JWT en `authToken`; el login
vuelve a guardar su JWT en `authToken` y `GET /me` lo usa como Bearer. Por eso no hace falta editar
nada antes de correr el Collection Runner.

### Orden y resultado esperado

| # | Carpeta / pedido | Método y ruta | Resultado esperado |
|---|---|---|---|
| 1 | 1. Flujo feliz → Registrar usuario | `POST /api/v1/users` | `201`, `AuthResponse`: `user` (`id`, `name`, `email`, `createdAt`) y `token` no vacío de 3 segmentos; guarda `userId`, `userEmail` y `authToken` |
| 2 | 1. Flujo feliz → Iniciar sesión | `POST /api/v1/auth/login` | `200`, `AuthResponse` con el mismo `user.id` y `token` de 3 segmentos; guarda `authToken` |
| 3 | 1. Flujo feliz → Consultar usuario por id | `GET /api/v1/users/{{userId}}` | `200`, mismo usuario, sin `password` ni `passwordHash` |
| 4 | 1. Flujo feliz → Consultar mi perfil | `GET /api/v1/users/me` con `Bearer {{authToken}}` | `200`, perfil del token: `id = {{userId}}`, sin `password` ni `passwordHash` |
| 5 | 2. Errores → Alta con email inválido | `POST /api/v1/users` | `400`, `code = VALIDATION_ERROR`, `details[0].field = email` |
| 6 | 2. Errores → Alta con JSON mal formado | `POST /api/v1/users` | `400`, `code = BAD_REQUEST`, `details` vacío |
| 7 | 2. Errores → Alta con email duplicado | `POST /api/v1/users` | `409`, `code = CONFLICT`, mensaje `Email ya registrado` |
| 8 | 2. Errores → Consultar usuario inexistente | `GET /api/v1/users/6710f3a2c9e77c00ffffffff` | `404`, `code = NOT_FOUND` |
| 9 | 2. Errores → Login con credenciales inválidas | `POST /api/v1/auth/login` con contraseña incorrecta | `401`, `code = UNAUTHORIZED`, mensaje `Credenciales inválidas`, `details` vacío |
| 10 | 2. Errores → Perfil sin token | `GET /api/v1/users/me` sin `Authorization` | `401`, `code = UNAUTHORIZED`, `details` vacío |
| 11 | 3. Salud → Health check | `GET /actuator/health` | `200`, `status = UP` |

**Importante:** el pedido 7 reutiliza el email creado en el pedido 1 y el pedido 4 reutiliza su
token. Si se ejecutan sueltos, el duplicado puede devolver `400` porque `{{userEmail}}` está vacío y
`/me` puede devolver `401` porque `{{authToken}}` no existe. Correr la colección completa en orden.

### JWT y perfil autenticado

El alta y el login devuelven `token` además del perfil `user`. Los tests verifican el status (`201` y
`200`), que el token sea un string no vacío con tres segmentos separados por puntos, que `user.id`
coincida con el registrado y que no aparezca `password` ni `passwordHash` en la respuesta. El pedido
`GET /api/v1/users/me` usa `Authorization: Bearer {{authToken}}` y comprueba que devuelve el perfil
del `id` registrado. Los errores de login y de token ausente o inválido se validan con `401
UNAUTHORIZED` y `details` vacío. No copiar tokens reales a tickets, capturas ni repositorios.

### Collection Runner

1. En Postman, elegir la colección **auth-service** → **Run**.
2. Seleccionar el entorno **auth-service - local**.
3. Ejecutar en orden (Run auth-service). Todos los tests deben quedar en verde.

La respuesta `500 INTERNAL_ERROR` existe en el contrato y está documentada en Swagger/`api.md`, pero
no se puede provocar desde un cliente sano: no hay un pedido de la colección para forzarla.

### Línea de comandos (opcional)

Si está instalado `newman`:

```powershell
newman run delivery/postman/auth-service.postman_collection.json `
  -e delivery/postman/auth-service.postman_environment.json
```

## Bruno

### Abrir la colección nativa

1. Bruno → **Open Collection**.
2. Elegir la carpeta `delivery/bruno/auth-service/`.
3. Seleccionar el entorno **local** (archivo `environments/local.bru`).
4. Ejecutar la colección completa (**Run Collection**) o los pedidos en el orden del árbol:
   `1-flujo-feliz` → `2-errores` → `3-salud`.

Los pedidos, nombres, orden y datos son los mismos que en Postman. El alta genera `userEmail` y
guarda `userId` y `authToken` en variables de la corrida; el login y `/me` reutilizan esas variables,
y el pedido de email duplicado depende del alta previa. El JWT del ejemplo es un marcador: el valor
real cambia en cada ejecución.

### Importar desde Postman u OpenAPI

Bruno también puede importar `delivery/postman/auth-service.postman_collection.json`
(**Import Collection → Postman Collection**) o `delivery/openapi/openapi.json`
(**Import Collection → OpenAPI**). La colección nativa es la recomendada porque incluye las
aserciones y variables ya configuradas.

## Validación de los archivos

Los JSON deben ser válidos. Con Node.js instalado:

```powershell
node -e "for (const f of process.argv.slice(1)) { JSON.parse(require('fs').readFileSync(f,'utf8')); console.log('OK ' + f); }" delivery/postman/auth-service.postman_collection.json delivery/postman/auth-service.postman_environment.json delivery/openapi/openapi.json delivery/bruno/auth-service/bruno.json
```

## Reiniciar los datos de prueba

Los tests crean usuarios reales en la base configurada. Para dejar la base limpia en local:

```powershell
mongosh $env:MONGO_URI --quiet --eval 'db.getSiblingDB(process.env.MONGO_DATABASE).users.deleteMany({})'
```

No ejecutar en una base compartida o de producción. La colección no necesita datos previos: el alta
genera un email nuevo en cada corrida.

## Problemas frecuentes

| Síntoma | Causa probable | Qué revisar |
|---|---|---|
| El servicio no arranca | Falta `MONGO_URI`, `MONGO_DATABASE` o `JWT_SECRET` | Definir las variables obligatorias; no hay valores por defecto |
| El arranque falla por JWT | `JWT_SECRET` mide menos de 32 bytes o no es válida para HS256 | Usar una clave de al menos 32 caracteres; revisar `JwtConfigTest` |
| `Connection refused` en Postman | El servicio no está escuchando en `baseUrl` | Puerto, perfil y logs de arranque |
| `health` devuelve `DOWN` | MongoDB no accesible | `MONGO_URI`, contenedor de Mongo y credenciales del entorno |
| El alta devuelve `409` al repetir | El email ya existe | Usar otro email o limpiar la base de prueba |
| El pedido de duplicado devuelve `400` | Se ejecutó sin el alta previa | Correr la colección completa en orden |
| `GET /me` devuelve `401` | Se ejecutó sin login/alta previa o el token venció | Correr la colección completa en orden o repetir el login |
| `verify` falla con `Could not find a valid Docker environment` | Docker no está accesible para Maven | Arrancar Docker y revisar `delivery/testing.md` |
| Swagger UI no abre en `prod` | Deshabilitado a propósito | Usar perfil local/dev o `delivery/openapi/` |
