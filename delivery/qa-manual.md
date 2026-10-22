# auth-service — manual de QA

Guía para validar el servicio con las colecciones de Postman y Bruno. Todos los pedidos apuntan a
los endpoints reales del código y usan los mismos ejemplos que Swagger y `delivery/api.md`.

## Antes de empezar

1. MongoDB 7 disponible y el servicio arrancado con las variables obligatorias:

   ```powershell
   Set-Location services/auth-service
   $env:MONGO_URI = 'mongodb://localhost:27017'
   $env:MONGO_DATABASE = 'auth_local'
   $env:SPRING_PROFILES_ACTIVE = 'dev'
   .\mvnw.cmd spring-boot:run
   ```

2. Verificar que responde:

   ```powershell
   Invoke-WebRequest http://localhost:8080/actuator/health
   ```

   Debe devolver `200` con `{"status":"UP"}`. Si el arranque falla, revisar que `MONGO_URI` y
   `MONGO_DATABASE` estén definidas y que MongoDB sea accesible.

3. La colección usa `baseUrl = http://localhost:8080`. Si el servicio corre en otro puerto, cambiar
   la variable `baseUrl` en el entorno.

## Postman

### Importar

1. Abrir Postman → **Import**.
2. Importar `delivery/postman/auth-service.postman_collection.json`.
3. Importar `delivery/postman/auth-service.postman_environment.json`.
4. Seleccionar el entorno **auth-service - local** en el selector superior derecho.

La colección trae variables propias (`baseUrl`, `userId`, `userEmail`). El pedido de alta genera un
email único por corrida y guarda el `id` en `userId`; por eso no hace falta editar nada antes de
correr el Collection Runner.

### Orden y resultado esperado

| # | Carpeta / pedido | Método y ruta | Resultado esperado |
|---|---|---|---|
| 1 | 1. Flujo feliz → Registrar usuario | `POST /api/v1/users` | `201`, devuelve `id`, `name`, `email`, `createdAt`; guarda `userId` |
| 2 | 1. Flujo feliz → Consultar usuario por id | `GET /api/v1/users/{{userId}}` | `200`, mismo usuario, sin `password` ni `passwordHash` |
| 3 | 2. Errores → Alta con email inválido | `POST /api/v1/users` | `400`, `code = VALIDATION_ERROR`, `details[0].field = email` |
| 4 | 2. Errores → Alta con JSON mal formado | `POST /api/v1/users` | `400`, `code = BAD_REQUEST`, `details` vacío |
| 5 | 2. Errores → Alta con email duplicado | `POST /api/v1/users` | `409`, `code = CONFLICT`, mensaje `Email ya registrado` |
| 6 | 2. Errores → Consultar usuario inexistente | `GET /api/v1/users/6710f3a2c9e77c00ffffffff` | `404`, `code = NOT_FOUND` |
| 7 | 3. Salud → Health check | `GET /actuator/health` | `200`, `status = UP` |

**Importante:** el pedido 5 reutiliza el email creado en el pedido 1. Si se ejecuta solo (sin el alta
previa en la misma corrida), puede devolver `400` en lugar de `409` porque `{{userEmail}}` está vacío.
Correr la colección completa en orden.

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
guarda `userId` en variables de la corrida; el pedido de email duplicado depende de esa alta previa.

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
| El servicio no arranca | Falta `MONGO_URI` o `MONGO_DATABASE` | Definir ambas variables; no hay valores por defecto |
| `Connection refused` en Postman | El servicio no está escuchando en `baseUrl` | Puerto, perfil y logs de arranque |
| `health` devuelve `DOWN` | MongoDB no accesible | `MONGO_URI`, contenedor de Mongo y credenciales del entorno |
| El alta devuelve `409` al repetir | El email ya existe | Usar otro email o limpiar la base de prueba |
| El pedido de duplicado devuelve `400` | Se ejecutó sin el alta previa | Correr la colección completa en orden |
| Swagger UI no abre en `prod` | Deshabilitado a propósito | Usar perfil local/dev o `delivery/openapi/` |
