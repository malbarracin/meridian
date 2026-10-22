---
title: "Auth Service — Arquitectura H2.4: MongoDB en entorno local"
status: borrador
source: specs/infra/mongodb-local.md + specs/auth/arquitectura-tests-mongodb.md
  + código existente en services/auth-service
updated: 2026-10-22T20:09:23.121Z
---

# Auth Service — Arquitectura H2.4: MongoDB en entorno local

## Fuente

- Especificación funcional: `specs/infra/mongodb-local.md` (H2.4 — MongoDB en entorno local)
- Arquitectura previa: `specs/auth/arquitectura-tests-mongodb.md` (H2.3)
- `project.yaml`: `java.group_id: com.meridian`, `java.base_package: com.meridian.users`
- Código existente: `services/auth-service` (rama `opensoma/20261022-f777`)

---

## 1. Contexto: qué ya existe (H2.1–H2.3)

Los hitos anteriores ya entregaron la infraestructura Docker que cubre la mayor parte de H2.4:

| Componente | Estado | Fichero |
|-----------|--------|---------|
| `docker-compose.yml` con servicio MongoDB 7 | ✅ intacto | `services/auth-service/docker-compose.yml` |
| Healthcheck MongoDB (`mongosh --eval ping`) | ✅ intacto | ídem |
| Volumen nombrado `mongodb-data` | ✅ intacto | ídem |
| `depends_on: condition: service_healthy` en auth | ✅ intacto | ídem |
| URI construida por Compose y `MONGO_DATABASE` vía `.env` | ✅ integrado | `services/auth-service/docker-compose.yml` |
| `.env` excluido de git (`.gitignore`) | ✅ intacto | `services/auth-service/.gitignore` |
| `/actuator/health` con indicador MongoDB | ✅ intacto | `application.yml` |
| `devops-local.md` con comandos operativos básicos | ✅ existe — **ampliar** | `delivery/devops-local.md` |

Lo que H2.4 **agrega**:

| Qué | Tipo |
|-----|------|
| Variables `MONGO_USER` y `MONGO_PASSWORD` en `.env.example` | MODIFICAR |
| Autenticación MongoDB obligatoria en `docker-compose.yml` local | MODIFICAR |
| Sección "Runbook de diagnóstico y reset" en `devops-local.md` | NUEVO en doc |
| Sección "Persistencia y reset de datos" en `devops-local.md` | NUEVO en doc |

No se toca código Java (`application.yml`, pom, ni ningún `.java`): H2.4 es exclusivamente infraestructura Compose y documentación operativa.

---

## 2. Decisión: autenticación MongoDB en desarrollo local

La especificación menciona `MONGO_USER` y `MONGO_PASSWORD` como variables de entorno esperadas.
El alcance excluye TLS/SSL y configuración productiva. **Decisión**: añadir autenticación básica
(`SCRAM-SHA-256`) al servicio MongoDB de Compose vía `MONGO_INITDB_ROOT_USERNAME` /
`MONGO_INITDB_ROOT_PASSWORD`. Las variables `MONGO_USER`, `MONGO_PASSWORD`, `MONGO_DATABASE`
y `JWT_SECRET` son obligatorias y no tienen defaults en Compose; no altera el comportamiento de los tests
(Testcontainers levanta su propio contenedor sin auth).

Compose construye `MONGODB_URI` y su alias `MONGO_URI` con las credenciales, la base de datos,
el host `mongodb` y `authSource=admin`. No se define una URI alternativa en `.env.example`.

> **Sin impacto en tests**: los tests de integración Testcontainers usan `@DynamicPropertySource`
> con la URI del contenedor propio; no leen `MONGO_URI` del `docker-compose.yml`.

---

## 3. Identidad del servicio

| Parámetro | Valor | Fuente |
|-----------|-------|--------|
| `base_package` | `com.meridian.users` | `project.yaml java.base_package` |
| `groupId` | `com.meridian` | `project.yaml java.group_id` |
| `artifactId` | `auth-service` | existente |
| Persistencia | MongoDB 7 | existente |
| API versioning | **deshabilitado** | `api.versioning` ausente en `project.yaml` |

---

## 4. Cambios en `docker-compose.yml`

Modificar **solo** el servicio `mongodb` y las variables de `auth-service`:

### 4.1 Servicio `mongodb` — agregar autenticación

```yaml
mongodb:
  image: mongo:7
  ports:
    - "127.0.0.1:27017:27017"
  environment:
    MONGO_INITDB_ROOT_USERNAME: ${MONGO_USER:?Set MONGO_USER in .env}
    MONGO_INITDB_ROOT_PASSWORD: ${MONGO_PASSWORD:?Set MONGO_PASSWORD in .env}
    MONGO_INITDB_DATABASE: ${MONGO_DATABASE:?Set MONGO_DATABASE in .env}
  volumes:
    - mongodb-data:/data/db
  healthcheck:
    test: [CMD, mongosh, --quiet, --eval, "const admin = db.getSiblingDB('admin'); admin.auth(process.env.MONGO_INITDB_ROOT_USERNAME, process.env.MONGO_INITDB_ROOT_PASSWORD); if (admin.runCommand({ping: 1}).ok !== 1) quit(1)"]
    interval: 10s
    timeout: 5s
    start_period: 20s
    retries: 5
```

> El `healthcheck` debe autenticarse: sin credenciales `mongosh` falla con auth habilitada.
> Se usan los mismos valores que `MONGO_INITDB_ROOT_*`.

### 4.2 `MONGO_URI` en `auth-service`

Construir las dos variables de entorno con la misma URI autenticada:

```yaml
MONGODB_URI: &mongodb-uri mongodb://${MONGO_USER:?Set MONGO_USER in .env}:${MONGO_PASSWORD:?Set MONGO_PASSWORD in .env}@mongodb:27017/${MONGO_DATABASE:?Set MONGO_DATABASE in .env}?authSource=admin
MONGO_URI: *mongodb-uri
```

La URI se deriva de las variables obligatorias de §5; no se proporciona un default.

---

## 5. Cambios en `.env.example`

Agregar `MONGO_USER` y `MONGO_PASSWORD` vacíos y dejar `JWT_SECRET` vacío como marcadores
no utilizables. Completar los tres en el `.env` privado antes del arranque:

```dotenv
# Copy to .env (ignored by Git) and fill all empty required values before startup.
# Use URI-safe letters/digits for credentials; Compose builds the authenticated URI.
MONGO_USER=
MONGO_PASSWORD=
MONGO_DATABASE=auth_local
PORT=8080
# Required: generate a local secret of at least 32 ASCII bytes.
JWT_SECRET=
# Optional token lifetime in milliseconds (default: 24 hours).
JWT_EXPIRATION_MS=86400000
```

> `MONGO_USER`, `MONGO_PASSWORD`, `MONGO_DATABASE` y `JWT_SECRET` son obligatorias, sin defaults
> en Compose. Generar credenciales propias y una clave JWT de al menos 32 bytes ASCII.
> No versionar secretos; guardarlos únicamente en el `.env` privado ignorado por Git.

---

## 6. Runbook de diagnóstico y reset — sección nueva en `devops-local.md`

Añadir la sección **"Runbook MongoDB — diagnóstico y reset"** a `delivery/devops-local.md`.
No reemplazar ni reformatear el contenido existente: insertar la sección al final del bloque
"Compose" existente (o como sección propia inmediatamente después).

### Contenido de la sección

#### Estado del stack y conectividad

```powershell
# Estado de los contenedores y healthcheck
docker compose ps
docker compose logs mongodb --tail 50
docker compose logs auth-service --tail 50

# Verificar que MongoDB responde (desde host, puerto publicado en loopback)
$env:MONGO_USER = (Get-Content .env | Select-String 'MONGO_USER').Line.Split('=')[1]
$env:MONGO_PASSWORD = (Get-Content .env | Select-String 'MONGO_PASSWORD').Line.Split('=')[1]
mongosh --host localhost --username $env:MONGO_USER --password $env:MONGO_PASSWORD --authenticationDatabase admin --quiet --eval "db.adminCommand('ping')"

# Verificar health de Auth Service
Invoke-WebRequest http://localhost:8080/actuator/health
```

Linux/macOS equivalente:
```bash
MONGO_USER=$(grep MONGO_USER .env | cut -d= -f2)
MONGO_PASSWORD=$(grep MONGO_PASSWORD .env | cut -d= -f2)
mongosh --host localhost --username "$MONGO_USER" --password "$MONGO_PASSWORD" \
  --authenticationDatabase admin --quiet --eval "db.adminCommand('ping')"
curl -s http://localhost:8080/actuator/health | jq .
```

#### Diagnóstico de falla de conexión

```powershell
# 1. Verificar que el contenedor mongo está healthy (STATUS = healthy)
docker compose ps

# 2. Ver logs de inicio de MongoDB (errores de auth, permisos, puerto)
docker compose logs mongodb --tail 100

# 3. Ver por qué Auth Service falla al conectar
docker compose logs auth-service --tail 100

# 4. Verificar variables obligatorias sin imprimir la URI ni secretos
docker compose config --quiet

# 5. Probar URI directamente desde el contenedor Auth (requiere mongosh en alpine)
# Alternativa: desde host en localhost:27017 (ver arriba)
```

Causas habituales:

| Síntoma | Causa | Solución |
|---------|-------|---------|
| Auth no arranca y MongoDB `starting` | MongoDB tarda en inicializar | Esperar — `depends_on` reintenta; si persiste, aumentar `start_period` |
| `Authentication failed` en logs de Auth | `MONGO_URI` sin credenciales o credenciales distintas a las de `MONGO_INITDB_ROOT_*` | Verificar `.env`: `MONGO_USER`, `MONGO_PASSWORD` y `MONGO_URI` deben coincidir |
| `Connection refused` | MongoDB no está corriendo o `MONGO_URI` apunta a host incorrecto | Dentro de Compose: `MONGO_URI` debe usar `mongodb` (nombre del servicio), no `localhost` |
| `/actuator/health` devuelve `DOWN` para MongoDB | Driver no puede alcanzar MongoDB | Revisar `MONGO_URI` y estado del contenedor MongoDB |

#### Persistencia — verificar que los datos sobreviven reinicios

```powershell
# Reiniciar solo Auth Service (sin tocar MongoDB ni el volumen)
docker compose restart auth-service

# Comprobar que el usuario de demo sigue disponible (usa el JWT del login previo)
$login = Invoke-RestMethod http://localhost:8080/api/v1/auth/login `
  -Method Post -ContentType 'application/json' `
  -Body '{"email":"ana.perez@example.com","password":"password123"}'
Invoke-RestMethod http://localhost:8080/api/v1/users/me `
  -Headers @{ Authorization = "Bearer $($login.token)" }
```

#### Reset de datos de demo a estado inicial

```powershell
# Detener el stack (NO borra el volumen)
docker compose down

# Detener y BORRAR el volumen (reset completo de datos de demo)
docker compose down --volumes

# Volver a levantar desde cero
docker compose up --build -d --wait
docker compose ps
Invoke-WebRequest http://localhost:8080/actuator/health
```

Linux/macOS:
```bash
docker compose down --volumes
docker compose up --build -d --wait
```

> `docker compose down` sin `--volumes` detiene los contenedores pero conserva `mongodb-data`.
> `docker compose down --volumes` elimina el volumen y todos los datos de demo.
> Usar `--volumes` solo cuando se quiere volver al estado vacío inicial.

---

## 7. Árbol de archivos — cambios en H2.4

Solo se modifican o crean archivos de infraestructura y documentación:

```
services/auth-service/
├── docker-compose.yml           [MODIFICAR] agregar auth MongoDB (MONGO_INITDB_ROOT_*, healthcheck)
└── .env.example                 [MODIFICAR] marcadores vacíos para credenciales y JWT_SECRET

delivery/
└── devops-local.md              [MODIFICAR] agregar sección runbook diagnóstico y reset
```

No se toca ningún archivo Java, `pom.xml`, `application.yml`, tests ni workflow de CI.

---

## 8. Flujo de arranque con autenticación

```
developer: docker compose up --build -d --wait
  → Compose lee .env
  → inicia mongodb con MONGO_INITDB_ROOT_USERNAME/PASSWORD
  → healthcheck autentica con mongosh --username --password
  → mongodb: healthy
  → inicia auth-service con MONGO_URI construida con credenciales, base y authSource=admin
  → auth-service conecta a MongoDB autenticado
  → /actuator/health → { status: UP, mongodb: { status: UP } }
```

---

## 9. Impacto en tests existentes

**Ninguno.** Los tests de integración (Testcontainers) levantan su propio `MongoDBContainer` sin
autenticación y se conectan vía `@DynamicPropertySource`. No dependen de `docker-compose.yml` ni de
`.env.example`. Los tests unitarios y `@WebMvcTest` usan Flapdoodle o mocks. Ningún test lee
`MONGO_USER`, `MONGO_PASSWORD` ni el `docker-compose.yml`.

La suite `./mvnw -q -B verify` pasa sin cambios.

---

## 10. Restricciones y fuera de alcance

| Fuera de alcance H2.4 |
|-----------------------|
| Configuración de MongoDB para entornos productivos o staging |
| Autenticación MongoDB con TLS/SSL |
| Replica sets o sharding |
| Persistencia productiva o multi-tenant |
| Cambios en código Java (application.yml, pom, clases) |

---

## 11. Riesgos y decisiones para el Dev

| # | Riesgo / decisión | Recomendación |
|---|-------------------|---------------|
| 1 | **Volumen ya inicializado sin auth**: si el desarrollador tiene un volumen `mongodb-data` previo (sin autenticación), al levantar Compose con `MONGO_INITDB_ROOT_*` MongoDB no re-inicializa las credenciales (las variables solo aplican al primer arranque del volumen). | Documentar: hacer `docker compose down --volumes` antes de actualizar a esta versión, o conectarse con `mongosh` y crear el usuario manualmente. El runbook cubre este caso. |
| 2 | **Healthcheck con credenciales** | El healthcheck debe pasar las mismas credenciales que `MONGO_INITDB_ROOT_*`. Si el desarrollador cambia solo una de las dos, el healthcheck falla. Documentar que deben coincidir. |
| 3 | **`mongosh` no disponible en el host** | El healthcheck corre dentro del contenedor MongoDB (que sí tiene `mongosh`). Los comandos de diagnóstico que usan `mongosh` desde el host son opcionales; si no está instalado, usar `docker compose exec mongodb mongosh`. |
| 4 | **CI sin impacto** | El workflow de CI ejecuta `./mvnw -q -B verify` en GitHub Actions sin levantar `docker-compose.yml`. No hace falta actualizar el workflow. |

---

## 12. Checklist antes de terminar

- [ ] `docker-compose.yml` agrega `MONGO_INITDB_ROOT_*` y healthcheck con credenciales
- [ ] `.env.example` incluye marcadores vacíos para `MONGO_USER`, `MONGO_PASSWORD` y `JWT_SECRET`
- [ ] `.env` sigue excluido de git (`.gitignore` no se toca — ya tiene `.env`)
- [ ] `devops-local.md` tiene la sección de runbook con diagnóstico y reset
- [ ] Los cambios de Compose no afectan los tests (Testcontainers/Flapdoodle)
- [ ] `docker compose config --quiet` pasa con un `.env` privado completo; sin variables obligatorias falla
