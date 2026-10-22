# Runbook — MongoDB local de auth-service (H2.4)

Guía operativa para el entorno **local y de demo** de `services/auth-service`: cómo levantar MongoDB 7
con Docker Compose, verificar que el stack está sano, diagnosticar fallas de conexión y volver los
datos de demo a estado inicial.

Alcance: solo local. Producción, staging, TLS, replica sets y MongoDB administrado están fuera de
alcance; para despliegue y Helm ver [`delivery/devops-local.md`](devops-local.md). La fuente de
requisitos es [`specs/infra/mongodb-local.md`](../specs/infra/mongodb-local.md).

Todos los comandos parten de `services/auth-service`, donde viven `docker-compose.yml` y `.env`.

## Archivos y variables

| Archivo | Para qué |
|---|---|
| `services/auth-service/docker-compose.yml` | Servicios `mongodb` (imagen `mongo:7`) y `auth-service`, volumen `mongodb-data`, healthchecks |
| `services/auth-service/.env.example` | Plantilla versionada de variables; las credenciales se entregan vacías |
| `services/auth-service/.env` | Valores reales del entorno local; **no se versiona** (excluido por `.gitignore`) |

Compose construye la URI de conexión; no se escribe a mano en `.env`:

```text
mongodb://${MONGO_USER}:${MONGO_PASSWORD}@mongodb:27017/${MONGO_DATABASE}?authSource=admin
```

| Variable | Dónde se define | Quién la usa | Valor |
|---|---|---|---|
| `MONGO_USER` | `.env` | MongoDB (usuario root) y la URI de Compose | Requerida, sin valor por defecto |
| `MONGO_PASSWORD` | `.env` | MongoDB (contraseña root) y la URI de Compose | Requerida, sin valor por defecto |
| `MONGO_DATABASE` | `.env` | Base de datos de la app y de la URI | Requerida; el ejemplo trae `auth_local` |
| `MONGODB_URI` | `docker-compose.yml` | Contrato de configuración por entorno de la spec (H2.4) | La arma Compose; no se define en `.env` |
| `MONGO_URI` | `docker-compose.yml` | La aplicación Spring (`spring.data.mongodb.uri`) | Alias de `MONGODB_URI`, mismo valor |
| `JWT_SECRET` | `.env` | Firma de los JWT; mínimo 32 bytes ASCII | Requerida, sin valor por defecto |
| `JWT_EXPIRATION_MS` | `.env` | Vigencia del JWT en milisegundos | Opcional; `86400000` (24 h) |
| `PORT` | `.env` | Puerto del host que publica Auth | Opcional; `8080` (el contenedor siempre escucha en 8080) |

Notas:

- Usar solo letras y dígitos en `MONGO_USER` y `MONGO_PASSWORD`: Compose inserta los valores tal cual
  en la URI y los caracteres reservados requerirían codificación porcentual distinta de la contraseña
  original de MongoDB.
- Las variables obligatorias usan `${VAR:?mensaje}`: si falta una, Compose falla antes de arrancar con
  `required variable MONGO_USER is missing a value: Set MONGO_USER in .env` (y equivalentes para el
  resto). No hay credenciales ni URI por defecto.
- Compose lee `.env` del directorio del `docker-compose.yml`. Una variable exportada en la terminal
  tiene prioridad sobre `.env`; si el puerto o las credenciales no coinciden, revisar el entorno del
  shell (`Get-ChildItem Env:PORT`, `Get-ChildItem Env:MONGO_*`).
- No compartir la salida de `docker compose config` sin `--quiet`: imprime la URI con credenciales.
  `docker compose config --quiet` valida la interpolación sin mostrar valores.

## Arranque

```powershell
Set-Location services/auth-service
Copy-Item .env.example .env
# Completar en .env: MONGO_USER, MONGO_PASSWORD y JWT_SECRET (mínimo 32 bytes ASCII).
docker compose config --quiet
docker compose up --build -d --wait
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

Linux/macOS:

```bash
cd services/auth-service
cp .env.example .env
# Completar en .env: MONGO_USER, MONGO_PASSWORD y JWT_SECRET.
docker compose config --quiet
docker compose up --build -d --wait
docker compose ps
curl -s http://localhost:8080/actuator/health
```

Resultado esperado:

- `docker compose ps` muestra los dos servicios en `Up ... (healthy)` y MongoDB publicado solo en
  loopback (`127.0.0.1:27017->27017/tcp`).
- Auth no arranca hasta que MongoDB responde el ping autenticado (`depends_on` con
  `condition: service_healthy`).
- `/actuator/health` responde `{"status":"UP"}`. El detalle de los indicadores (incluido MongoDB) está
  oculto por configuración; para diagnosticar se usan los logs y los comandos de abajo.
- El puerto del host es el de `PORT` en `.env`; si se cambió, usar ese puerto en las URLs.

## Diagnóstico

### Estado de los servicios

```powershell
docker compose ps
docker compose logs --tail 100 mongodb
docker compose logs --tail 100 auth-service
docker inspect --format '{{.State.Health.Status}}' $(docker compose ps -q mongodb)
```

El healthcheck de MongoDB (cada 10 s, tras 20 s de gracia, 5 reintentos) se autentica con las mismas
credenciales `MONGO_INITDB_ROOT_*` y ejecuta un `ping`. El de Auth consulta
`http://127.0.0.1:8080/actuator/health` cada 30 s, con 60 s de gracia.

### Conectividad con MongoDB

El comando se ejecuta **dentro** del contenedor, que tiene `mongosh` y las credenciales en su entorno:

```powershell
docker compose exec -T mongodb mongosh --quiet --eval "const admin = db.getSiblingDB('admin'); admin.auth(process.env.MONGO_INITDB_ROOT_USERNAME, process.env.MONGO_INITDB_ROOT_PASSWORD); printjson(admin.runCommand({ping: 1}))"
```

Respuesta esperada: `{ ok: 1 }`. Cualquier error de autenticación en este comando indica que las
credenciales de `.env` no coinciden con las del volumen.

No hace falta instalar `mongosh` en la máquina: si se quiere usar desde el host, apuntar a
`localhost:27017` con `--authenticationDatabase admin`; desde el host el nombre `mongodb` no resuelve.

### Auth Service

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
docker compose logs --tail 100 auth-service
```

Comprobar en privado que `.env` contiene las variables obligatorias con `docker compose config --quiet`.
No volcar el entorno del contenedor: `MONGO_URI` incluye usuario y contraseña, y `JWT_SECRET` es secreto.
La app lee `MONGO_URI` y `MONGO_DATABASE`; dentro de Compose el host de MongoDB
es `mongodb`, mientras que desde la máquina anfitriona es `localhost`.

### Persistencia del volumen

```powershell
# Detener sin borrar el volumen y volver a levantar
docker compose down
docker compose up -d --wait

# Sonda directa en una base descartable (no toca auth_local)
docker compose exec -T mongodb mongosh --quiet --eval "const admin = db.getSiblingDB('admin'); admin.auth(process.env.MONGO_INITDB_ROOT_USERNAME, process.env.MONGO_INITDB_ROOT_PASSWORD); const probe = db.getSiblingDB('runbook_probe'); probe.marca.insertOne({_id: 'persistencia'}); print(probe.marca.countDocuments())"
```

Después de `down`/`up` la sonda debe devolver `1`: el volumen nombrado `mongodb-data` conserva los
datos. Para comprobar el camino completo por la API (requiere el stack construido):

```powershell
$body = @{ name = 'Persistencia'; email = ('persist-' + [guid]::NewGuid().ToString('N') + '@example.com'); password = 'SmokeTest123!' } | ConvertTo-Json
$created = Invoke-RestMethod http://localhost:8080/api/v1/users -Method Post -ContentType 'application/json' -Body $body
docker compose restart auth-service
docker compose up -d --wait
$login = Invoke-RestMethod http://localhost:8080/api/v1/auth/login -Method Post -ContentType 'application/json' -Body $body
if ($login.user.id -ne $created.user.id) { throw 'El usuario no coincide tras reiniciar Auth' }
```

## Reset de datos de demo

Vuelve al estado inicial: **elimina todos los datos** del volumen `mongodb-data`, incluidos los
usuarios registrados. No ejecutarlo si hay datos que se quieren conservar.

```powershell
docker compose down --volumes
docker compose up -d --wait
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

Linux/macOS:

```bash
docker compose down --volumes
docker compose up -d --wait
docker compose ps
curl -s http://localhost:8080/actuator/health
```

- `docker compose down` (sin `--volumes`) detiene y elimina los contenedores pero conserva el volumen.
- `docker compose down --volumes` (equivalente a `-v`) borra el volumen `mongodb-data` y los datos.
- El siguiente `up` crea un volumen vacío: MongoDB reaplica en ese primer arranque `MONGO_USER`,
  `MONGO_PASSWORD` y `MONGO_DATABASE` del `.env` vigente. Si se cambió alguna credencial, el reset es
  la forma de que tomen efecto.
- Añadir `--build` al `up` solo si cambió el código o la imagen; para un reset de datos no hace falta.

## Troubleshooting

| Síntoma | Causa probable | Solución |
|---|---|---|
| `required variable MONGO_USER is missing a value: Set MONGO_USER in .env` al ejecutar cualquier comando de Compose | Falta `.env`, está incompleto, o se ejecuta desde otra carpeta | Crear `.env` desde `.env.example`, completar `MONGO_USER`, `MONGO_PASSWORD` y `JWT_SECRET` y ejecutar desde `services/auth-service` |
| MongoDB queda `unhealthy` y el health log muestra `MongoServerError: Authentication failed.` | El volumen ya existía inicializado con otras credenciales; `MONGO_INITDB_ROOT_*` solo se aplica al crear el volumen | Para una demo descartable, `docker compose down --volumes` y volver a levantar; para conservar datos, crear/rotar el usuario con `mongosh` autenticado como administrador |
| Auth falla al conectar con `Authentication failed` mientras MongoDB está healthy | `MONGO_URI`/`MONGO_DATABASE` del contenedor no coinciden con las credenciales de MongoDB (por ejemplo, app fuera de Docker apuntando a otro usuario) o el contenedor quedó con variables viejas | Revisar `.env` en privado sin volcar variables, recrear con `docker compose up -d --force-recreate auth-service` y, fuera de Compose, usar una URI con las mismas credenciales y `authSource=admin` |
| `Connection refused` en los logs de Auth | MongoDB todavía no está healthy, o la URI apunta al host equivocado | `docker compose ps`, `docker compose logs mongodb`; dentro de Compose el host es `mongodb`, desde el host es `localhost` |
| `/actuator/health` responde `DOWN` | La app no alcanza MongoDB | Revisar `MONGO_URI` y el estado de MongoDB; el detalle del indicador está oculto, usar logs y el `ping` con `mongosh` |
| El arranque falla con `port is already allocated` para el puerto 27017 | Otro proceso o contenedor MongoDB usa el puerto 27017 | Detener el contenedor o proceso que lo ocupa (`Get-NetTCPConnection -LocalPort 27017`) antes de levantar el stack |
| El stack no responde en el puerto esperado o `docker compose config` muestra otro `published` | Una variable `PORT` exportada en el shell tiene prioridad sobre `.env` | `Get-ChildItem Env:PORT`; ajustar o quitar la variable del shell (`Remove-Item Env:PORT`) y volver a levantar |
| `up --wait` termina con código 1 y `container auth-service-mongodb-1 is unhealthy` | Alguno de los dos healthchecks no pasa | `docker inspect --format '{{json .State.Health}}' $(docker compose ps -q mongodb)` para leer el último resultado, y `docker compose logs` del servicio |
| MongoDB no arranca y los logs muestran errores de `WiredTiger` o del volumen | Volumen corrupto | Respaldar lo necesario, `docker compose down --volumes` y volver a levantar; si el volumen no se libera, `docker volume rm auth-service_mongodb-data` con el stack detenido |
| `docker info` falla con permiso denegado al daemon | Docker Desktop/Engine no está accesible para el usuario | Iniciar Docker y verificar `docker info` antes de usar Compose |
| No hay `mongosh` en la máquina | No es necesario | Usar `docker compose exec -T mongodb mongosh ...`, que corre dentro del contenedor |

Rutas de datos: el volumen nombrado es `mongodb-data` (prefijado por Compose como
`auth-service_mongodb-data`), montado en `/data/db`. No es un bind mount: los archivos no quedan en el
repositorio.

## Verificación de este runbook

Comprobado contra el `docker-compose.yml` real (TASK-38) el 2026-10-22, con Docker 29.5.3 y
`mongo:7`:

- `docker compose config --quiet` con las variables obligatorias completas: sin errores; sin ellas
  falla con el mensaje de variable requerida citado arriba.
- `docker compose up -d --wait mongodb`: MongoDB queda `Up (healthy)` con el puerto en loopback y el
  health log devuelve `{ ok: 1 }`.
- `docker compose exec -T mongodb mongosh ...` con autenticación desde el entorno del contenedor:
  `{ ok: 1 }`.
- Persistencia: una marca insertada en `runbook_probe` sigue presente tras `docker compose down` y
  `docker compose up -d --wait`.
- Reset: tras `docker compose down --volumes` y `docker compose up -d --wait`, el volumen nuevo está
  vacío (la sonda devuelve `0`) y MongoDB vuelve a `healthy`.
- Credenciales cambiadas con el volumen existente: el contenedor queda `unhealthy`, `up --wait`
  termina con código 1 y el health log muestra `MongoServerError: Authentication failed.`.

No verificado en este entorno: `docker compose up --build -d --wait` con el stack completo (requiere
construir la imagen de Auth con acceso a Maven y a las imágenes base) y el smoke por API de la sección
de persistencia, que depende de ese arranque. Comando pendiente: desde `services/auth-service`, con
`.env` completo, `docker compose up --build -d --wait` y luego el bloque de la API.

## Referencias

- [`delivery/devops-local.md`](devops-local.md) — Compose, CI, Helm y despliegue.
- [`delivery/testing.md`](testing.md) — tests con Testcontainers y diagnóstico de Docker.
- [`services/auth-service/README.md`](../services/auth-service/README.md) — configuración y API del servicio.
- [`specs/infra/mongodb-local.md`](../specs/infra/mongodb-local.md) — requisitos H2.4.
