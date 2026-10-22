# auth-service — tests locales con Testcontainers

Guía para ejecutar la suite de integración H2.3 contra un MongoDB real sin instalar ni administrar
MongoDB en la máquina. Aplica a `services/auth-service`.

## Requisitos

- JDK 21.
- Docker Engine o Docker Desktop con el daemon accesible para el usuario que corre Maven:
  `docker info` debe responder sin errores.
- Maven no hace falta instalarlo: se usa el wrapper del servicio (`.\mvnw.cmd` en Windows,
  `./mvnw` en Linux/macOS).
- No hace falta MongoDB local, `MONGO_URI`, `MONGO_DATABASE` ni `JWT_SECRET` para los tests: la
  configuración de test aporta la conexión y la clave pública de la fixture.

## Qué levanta la suite

`src/test/java/com/meridian/users/MongoTestContainer.java` es la base de los tests de integración:

- Levanta un contenedor `mongo:7.0.14` con Testcontainers 1.21.4 (`testcontainers-bom`).
- Expone la URI del replica set con `getReplicaSetUrl()` y la registra como
  `spring.data.mongodb.uri` mediante `@DynamicPropertySource`, antes de crear el contexto Spring.
- El contenedor es `static` y se comparte durante la JVM; `withReuse(false)` evita reutilizarlo entre
  ejecuciones. No usar `@Testcontainers`/`@Container` en las clases que heredan de esta base: detener
  el contenedor entre clases invalidaría las conexiones de los contextos Spring cacheados.
- Testcontainers elimina el contenedor al finalizar la JVM (Ryuk). No deshabilitar Ryuk.

Clases que lo usan:

| Clase | Qué ejercita |
|---|---|
| `AuthIntegrationTest` | Registro HTTP, seed explícito y recuperación tras una interrupción real de MongoDB |
| `LoginIntegrationTest` | `POST /api/v1/auth/login`: éxito con JWT y rechazos `401` |
| `ProfileIntegrationTest` | `GET /api/v1/users/me`: perfil persistido y rechazos `401` |

El resto de la suite (entidad, repositorio, servicio, mapper, controllers con `MockMvc`, contrato de
error, configuración, JWT y contexto) usa MongoDB embebido Flapdoodle 7.0.14 o mocks y no necesita
Docker.

## Comandos

Windows (PowerShell), desde la raíz del repositorio:

```powershell
Set-Location services/auth-service
docker info
.\mvnw.cmd -q -B clean verify
```

Linux/macOS:

```bash
cd services/auth-service
docker info
./mvnw -q -B clean verify
```

Desde la raíz, el wrapper delega al servicio:

```powershell
.\mvnw.cmd -q -B verify
```

Solo los tests de integración (sin el gate de cobertura):

```powershell
Set-Location services/auth-service
.\mvnw.cmd -q -B -Dtest='*IntegrationTest' test
```

Una sola clase o un solo método:

```powershell
.\mvnw.cmd -q -B -Dtest=LoginIntegrationTest test
.\mvnw.cmd -q -B -Dtest=ProfileIntegrationTest#meConTokenAusente401 test
```

El cierre del servicio es siempre `verify` completo: incluye JaCoCo (mínimo 80 % de líneas) y la
regeneración de `delivery/openapi/openapi.json` y `openapi.yaml`.

La primera corrida descarga la imagen `mongo:7.0.14` y las imágenes auxiliares de Testcontainers. Para
evitar esperas durante los tests se puede precargar:

```powershell
docker pull mongo:7.0.14
```

## Variables y perfiles

- `src/test/resources/application.yml` aporta la fixture pública de test: `jwt.secret` de ejemplo y
  `jwt.expiration-ms`. No contiene secretos reales.
- `src/main/resources/application-test.yml` activa `auto-index-creation: true` para el índice único de
  email. La URI efectiva la inyecta Testcontainers; no se usa una base compartida.
- **No exportar `JWT_SECRET` de forma global** al correr los tests: `JwtConfigTest` comprueba el
  arranque sin clave y un valor en el entorno rompe ese caso.
- Los tests escriben en un contenedor efímero: no apuntar la suite a una base compartida ni a
  producción.

## CI

`.github/workflows/auth-service.yml` corre en `ubuntu-latest` usando el socket Docker del runner
(host Docker, sin Docker-in-Docker). Antes de Maven verifica `docker version` y `docker info`; si
Docker no está disponible, el pipeline falla y no se omiten tests. Los reportes Surefire y JaCoCo se
publican en el artefacto `auth-service-test-reports`.

## Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `Could not find a valid Docker environment` | El daemon no está accesible para el proceso de Maven | Arrancar Docker Desktop/Engine, ejecutar `docker info` en la misma terminal y repetir `verify` |
| `permission denied` al conectar con `dockerDesktopLinuxEngine` | Falta permiso del usuario sobre el daemon (Windows) | Abrir una terminal nueva, verificar la sesión y los permisos de Docker Desktop; reiniciar Docker |
| Los tests se quedan esperando al inicio | Primera descarga de `mongo:7.0.14` o red lenta | Esperar la descarga o ejecutar `docker pull mongo:7.0.14` antes |
| Error al descargar la imagen | Proxy o registry bloqueado | Configurar el proxy en Docker y volver a intentar; verificar con `docker pull mongo:7.0.14` |
| Timeout al crear el contenedor | Firewall o antivirus bloqueando Testcontainers/Ryuk | Permitir Docker y los procesos de Testcontainers; no deshabilitar Ryuk |
| Quedan contenedores `mongo:7.0.14` | Una JVM terminó de forma anormal antes de la limpieza | Ver `docker ps -a --filter ancestor=mongo:7.0.14` y eliminar los huérfanos con `docker rm -f <id>` |

Si Docker no se puede habilitar en el entorno, la suite de integración no se puede verificar ahí: los
tests fallan al inicializar el contenedor (no se omiten ni se cambia el gate JaCoCo).
