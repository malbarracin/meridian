# Operación local de auth-service

Requisitos: Java 21, Docker con Compose v2 o posterior; Helm 3 y kubectl para Kubernetes.
Persistencia: MongoDB 7. Identidad y puerto tomados del POM y de specs/auth/arquitectura.md.
Todos los comandos PowerShell parten de la raíz del repositorio, salvo donde se indica.

## Compose

```powershell
Set-Location services/auth-service
Copy-Item .env.example .env
# Completar MONGO_USER, MONGO_PASSWORD y JWT_SECRET en .env antes de continuar.
docker compose config --quiet
docker compose up --build -d --wait
docker compose ps
Invoke-WebRequest http://localhost:8080/actuator/health
docker compose logs --tail 100 auth-service
docker compose down
Set-Location ../..
```

Completar `.env` antes de arrancar: `MONGO_USER`, `MONGO_PASSWORD` y `JWT_SECRET` se entregan
vacíos. `MONGO_DATABASE=auth_local` y `PORT=8080` son ejemplos de configuración local.
Compose construye `MONGODB_URI` y su alias compatible `MONGO_URI` con host `mongodb`,
base `MONGO_DATABASE` y `authSource=admin`. No se acepta una URI alternativa silenciosa.
Usar letras y dígitos para las credenciales de este entorno local: los caracteres reservados
en una URI requerirían codificación porcentual, diferente de la contraseña original de MongoDB.
`PORT` publica el puerto del host; la app dentro de Compose escucha en 8080.
H2.2 agrega `JWT_SECRET` (mínimo 32 bytes ASCII) y `JWT_EXPIRATION_MS` opcional
(86400000 ms = 24 horas). Generar una clave local de al menos 32 bytes ASCII en `.env`.
MongoDB conserva datos en el volumen `mongodb-data`; `down` no lo elimina.
MongoDB local usa autenticación y su puerto se publica solo en loopback para desarrollo.
No usar este Compose como configuración de producción.
La app espera a que el ping de MongoDB tenga éxito y ambos servicios tienen healthcheck.

La imagen usa JDK 21 alpine para compilar con el wrapper y JRE 21 alpine para ejecutar el JAR.
El usuario tiene UID/GID 10001. MONGO_URI y MONGO_DATABASE no tienen conexión predeterminada.
El build de imagen omite tests; CI los ejecuta antes del build Docker.

## Runbook MongoDB — diagnóstico, persistencia y reset

Runbook dedicado con el detalle de arranque, diagnóstico, persistencia y reset:
[`delivery/runbook-mongodb-local.md`](runbook-mongodb-local.md).

Desde `services/auth-service`, con `.env` completo:

```powershell
docker compose config --quiet
docker compose up --build -d --wait
docker compose ps
docker compose logs --tail 100 mongodb auth-service
docker compose exec mongodb mongosh --quiet --eval 'const admin = db.getSiblingDB("admin"); admin.auth(process.env.MONGO_INITDB_ROOT_USERNAME, process.env.MONGO_INITDB_ROOT_PASSWORD); admin.runCommand({ping: 1})'
Invoke-RestMethod http://localhost:8080/actuator/health
```

`config --quiet` verifica variables obligatorias sin imprimir credenciales. No compartir la
salida de `docker compose config` sin `--quiet` ni volcar las variables del contenedor.
Si aparece `Authentication failed`, comprobar las credenciales privadas de `.env` y si el
volumen fue inicializado con otros valores. `MONGO_INITDB_ROOT_*` solo se aplica al crear
un volumen vacío. Para conservar datos existentes, crear o rotar el usuario con `mongosh`
autenticado como administrador; para una demo descartable, usar el reset explícito de abajo.
Si hay `Connection refused`, revisar logs y `docker compose ps`: el host dentro de Compose
es `mongodb`; desde la máquina anfitriona es `localhost`. Mongo debe estar healthy antes de Auth.

Verificar persistencia con un usuario único (ajustar el puerto si se cambió `PORT`):

```powershell
$body = @{ name = 'Persistencia'; email = ('persist-' + [guid]::NewGuid().ToString('N') + '@example.com'); password = 'SmokeTest123!' } | ConvertTo-Json
$created = Invoke-RestMethod http://localhost:8080/api/v1/users -Method Post -ContentType 'application/json' -Body $body
docker compose restart auth-service
docker compose up -d --wait
$login = Invoke-RestMethod http://localhost:8080/api/v1/auth/login -Method Post -ContentType 'application/json' -Body $body
$profile = Invoke-RestMethod http://localhost:8080/api/v1/users/me -Headers @{ Authorization = ('Bearer ' + $login.token) }
if ($profile.id -ne $created.id) { throw 'El usuario no coincide tras reiniciar Auth' }
# Verificar también conservación del volumen al detener todo el stack:
docker compose down
docker compose up -d --wait
$login = Invoke-RestMethod http://localhost:8080/api/v1/auth/login -Method Post -ContentType 'application/json' -Body $body
```

Reset opcional: el siguiente comando **elimina todos los datos de demo** del volumen.
No ejecutarlo si se necesitan conservar los usuarios:

```powershell
docker compose down --volumes
docker compose up --build -d --wait
docker compose ps
```

Se conserva `mongo:7`, la versión mayor ya integrada por el equipo; no se cambia la versión
de persistencia ni la configuración productiva. El volumen nombrado sigue siendo `mongodb-data`.

## Maven y perfiles

```powershell
.\mvnw.cmd clean verify -B
.\mvnw.cmd dependency:tree -Dverbose -B
```

Linux/CI: `cd services/auth-service` y `./mvnw clean verify -B`.
No se versionan `target/`, reportes ni `.env`.
Los perfiles dev/docker/test/prod requieren MONGO_URI y MONGO_DATABASE por entorno, sin
fallback embebido. Los tests anteriores usan Flapdoodle; la suite H2.3 usa MongoDB 7.0.14
mediante Testcontainers y DynamicPropertySource. Requiere acceso al daemon Docker y descarga
las imágenes de MongoDB y de los auxiliares de Testcontainers si no están en caché.
El perfil test habilita índices, sin arrancar una base implícitamente.

App fuera de Docker (MongoDB de Compose en localhost):

```powershell
Set-Location services/auth-service
# Con .env ya completado; proporcionar las mismas credenciales en la URI local.
docker compose up -d mongodb
$env:MONGO_URI = Read-Host 'URI MongoDB autenticada (host localhost, authSource=admin)'
$env:MONGO_DATABASE = Read-Host 'Base de datos configurada en .env'
$env:SPRING_PROFILES_ACTIVE = 'dev'
$env:JWT_SECRET = 'dev-only-jwt-secret-change-me-32-chars-minimum'
.\mvnw.cmd spring-boot:run
# Tras detener la app con Ctrl+C:
Remove-Item Env:MONGO_URI, Env:MONGO_DATABASE, Env:SPRING_PROFILES_ACTIVE, Env:JWT_SECRET
docker compose down
Set-Location ../..
```

Dev y docker crean índices automáticamente. Prod mantiene auto-index-creation desactivado.
En prod, `UserIndexConfig` crea o confirma el índice único de email durante la inicialización
del contexto, antes de completar el arranque. La credencial de la aplicación necesita permisos
para crear índices. Si existen duplicados, un índice incompatible o faltan permisos, el arranque
falla y la instancia no queda lista para tráfico. El comando manual equivalente es:

```powershell
mongosh $env:MONGO_URI --quiet --eval 'db.getSiblingDB(process.env.MONGO_DATABASE).users.createIndex({email: 1}, {unique: true})'
```

## CI/CD

`.github/workflows/auth-service.yml` verifica cada push/PR que afecte el servicio, su workflow
o el chart, con Temurin 21, caché Maven y `./mvnw -q -B verify`. El job build-test
usa la configuración JWT pública de `src/test/resources/application.yml`. En push a main construye la
imagen con tag `ghcr.io/<org>/<repo>:<sha>`, tras pasar tests. No publica imágenes porque no
hay registry autorizado/configurado. Para push futuro: configurar permisos packages:write
y GITHUB_TOKEN, o credencial del registry en GitHub Secrets. No hay secretos reales en workflows.
`auth-service-cd.yml` es una plantilla manual informativa; no despliega. Configurar registry,
cluster, credenciales y environment protegido antes de agregar un deploy real.

H2.3 ejecuta los tests en el host `ubuntu-latest`, usando directamente el socket Docker
del runner (sin contenedor de job ni Docker-in-Docker). Antes de Maven, `docker version`
y `docker info` deben pasar. No se deshabilitan tests ni Ryuk; una falta de Docker falla
el pipeline. Surefire descubre las clases `*IntegrationTest` con el patrón `*Test`;
no hace falta un perfil Maven adicional. El workflow conserva los reportes Surefire y
JaCoCo incluso si falla la verificación, en el artefacto `auth-service-test-reports`.
La imagen se construye después de pasar la suite; el Dockerfile omite los tests dentro
del build porque ya se ejecutaron con Docker en el host.
No exportar `JWT_SECRET` al ejecutar tests: `JwtConfigTest.fallaArranqueSinClave` necesita
comprobar la ausencia de la clave en un contexto aislado. CI no define variables JWT globales.

H2.3 no agrega variables de ejecución: login y `/me` reutilizan `MONGO_URI`,
`MONGO_DATABASE`, `JWT_SECRET` y `JWT_EXPIRATION_MS`. Dockerfile, Compose y `.env.example`
existentes cubrían esa configuración. H2.4 agrega las credenciales MongoDB descritas arriba.

Para comprobar los endpoints de la imagen final, después del arranque con Compose:

```powershell
$body = @{ name = 'Smoke'; email = ('smoke-' + [guid]::NewGuid().ToString('N') + '@example.com'); password = 'SmokeTest123!' } | ConvertTo-Json
Invoke-RestMethod http://localhost:8080/api/v1/users -Method Post -ContentType 'application/json' -Body $body
$login = Invoke-RestMethod http://localhost:8080/api/v1/auth/login -Method Post -ContentType 'application/json' -Body $body
Invoke-RestMethod http://localhost:8080/api/v1/users/me -Headers @{ Authorization = ('Bearer ' + $login.token) }
```

El registro debe devolver 201, login 200 con token y `/me` 200 con el usuario registrado.
Usar el puerto de `.env` si se cambió el valor 8080.

## Helm

MongoDB es externo al chart. Por defecto se referencia el Secret `auth-mongodb` con la clave
`MONGO_URI`; aprovisionarlo mediante el gestor de secretos del entorno. Alternativamente,
vaciar `mongo.existingSecret` y pasar `mongo.uri` desde un archivo privado ignorado por Git
(por ejemplo `services/auth-service/.env.helm.yaml`), nunca desde valores versionados.
La base se configura con `mongo.database`; imagen, tag y imagePullSecrets son configurables.
Los valores por defecto permiten renderizar sin secretos reales; no crean el Secret externo.
H2.2 requiere además el Secret externo `auth-jwt` con clave `JWT_SECRET` de al menos
32 bytes, aprovisionado por el gestor de secretos antes de instalar. `jwt.existingSecret`
configura su nombre; `jwt.expirationMs` configura `JWT_EXPIRATION_MS` mediante ConfigMap
(86400000 por defecto). Los overrides dev/prod heredan estas opciones de `values.yaml`.
La clave llega al Deployment mediante `secretKeyRef`; el chart no almacena su valor.
Después de rotar un Secret externo, reiniciar el Deployment para cargar la clave nueva.

```powershell
helm lint helm/auth-service
helm template auth helm/auth-service -f helm/auth-service/values-dev.yaml
helm template auth helm/auth-service -f helm/auth-service/values-prod.yaml
```

Con cluster local y Secret ya aprovisionado:

```powershell
docker build -t auth-service:local services/auth-service
# Cargar la imagen en kind si corresponde:
kind load docker-image auth-service:local
helm upgrade --install auth helm/auth-service -f helm/auth-service/values-dev.yaml -n dev --create-namespace
kubectl rollout status deployment/auth-auth-service -n dev
kubectl port-forward service/auth-auth-service 8080:8080 -n dev
```

El chart usa perfil prod por defecto, ClusterIP, usuario no-root y probes startup/readiness/
liveness en `/actuator/health`. Requests/limits figuran en values y pueden ajustarse según carga.
Para producción, usar values-prod, fijar `image.repository` y `image.tag` a una imagen publicada,
aprovisionar el Secret con permisos de creación de índices antes del despliegue.
El servicio garantiza el índice durante el arranque. No se realizó deploy real.

## Verificación realizada

### Cierre H2.4 / TASK-38 (2026-10-22)

- `docker compose --env-file .env.example config --quiet`: pasó al proporcionar variables
  efímeras ficticias `MONGO_USER`, `MONGO_PASSWORD` y `JWT_SECRET` en el proceso de validación.
  Sin ellas, falla como corresponde: el ejemplo vacío exige completar credenciales.
- `.\mvnw.cmd -q -B verify` desde la raíz: 116 tests, 0 fallos, 12 errores de
  Testcontainers por acceso denegado a las named pipes Docker; 0 omitidos. El gate de
  cobertura no pudo completarse. No se cambió ni omitió ningún test.
- `.\mvnw.cmd -q dependency:tree -Dverbose`: código 0.
- `docker info`: permiso denegado a `dockerDesktopLinuxEngine`; no se arrancaron contenedores.
- `git diff --check`: pasó. `git check-ignore` confirma exclusión de `.env` en raíz y
  servicio; `.env.example` sigue versionado y no hay `.env` versionados.
- Cambios limitados a Compose, ejemplo de variables, exclusión de secretos y este runbook.
  Sin cambios Java, Maven, workflows ni Helm. Los comandos históricos con
  `--env-file .env.example` requieren ahora credenciales; usar el `.env` privado completo.

### Cierre H2.3 / TASK-34 (2026-10-22)

- `docker compose --env-file .env.example config --quiet`: pasó.
- YAML de workflow y Compose validado con SnakeYAML 2.4, rechazando claves duplicadas.
- `mvnw.cmd -q dependency:tree -Dverbose`: terminó con código 0.
- Se detectó y eliminó la configuración JWT global del workflow: hacía fallar el test
  de arranque sin clave. La fixture del classpath proporciona los valores para los demás tests.
- `mvnw.cmd -q -B clean verify`: intentado; Docker no está accesible desde este entorno.
  Resultado: 116 tests, 104 correctos, 0 fallos de aserción, 12 errores al inicializar
  Testcontainers por ausencia de Docker accesible; 0 omitidos.
  No se omitieron tests ni se modificó el gate JaCoCo. La suite completa y su cobertura
  quedan pendientes de un runner con Docker accesible.
- `git diff --check`: pasó. Sin cambios necesarios en Dockerfile/Compose ni variables nuevas.

### Cierre H2.2 / TASK-26 (2026-10-22)

- `docker compose --env-file .env.example config --quiet`: pasó con las variables JWT.
- `mvnw.cmd -q -B clean verify`: pasó, 65 tests sin fallos y gate JaCoCo 80%,
  con JWT_SECRET=test-secret-key-for-unit-tests-only-32chars y JWT_EXPIRATION_MS=3600000,
  los mismos valores públicos de la fixture que configura CI. La primera ejecución con
  otra clave de test falló porque UsersApplicationTest exige estos valores exactos;
  se corrigió el entorno de CI sin modificar tests.
- `mvnw.cmd -q dependency:tree -Dverbose`: pasó.
- YAML de workflow, Compose y values dev/prod validado con SnakeYAML 2.4,
  rechazando claves duplicadas; plantillas Go pendientes de Helm.
- Dockerfile existente validado por inspección: JDK 21 → JRE 21 alpine, usuario app,
  wrapper y JAR Spring Boot completo con dependencias runtime JJWT; no requiere cambios.
- El build Docker omite tests y no requiere JWT_SECRET durante compilación;
  la clave se inyecta únicamente al ejecutar el contenedor.
- El smoke correcto es `/actuator/health` y, opcionalmente, `POST /api/v1/users`;
  la ruta `/api/v1/auth/register` del AC original es obsoleta.

### Verificación previa H2.1

- `mvnw.cmd clean verify -B -q`: pasó con Java 21 y gate JaCoCo 80%.
- `mvnw.cmd dependency:tree -Dverbose -B`: BUILD SUCCESS.
- `docker compose --env-file .env.example config --quiet`: pasó.
- Sintaxis YAML validada con SnakeYAML 2.4 ya presente en el caché Maven: workflows,
  perfiles, Compose, Chart.yaml y values de Helm; rechazo de claves duplicadas habilitado.
  Las plantillas Go de Helm requieren Helm para su validación completa.
- `git diff --check`: pasó. Los archivos entregados solo contienen ejemplos sin credenciales.

## Pendiente de verificar

- H2.4 / TASK-38 AC #6 y #7: con acceso al daemon y `.env` completo, desde
  `services/auth-service`, ejecutar `docker compose up --build -d --wait`,
  `docker compose ps` y el script de registro/reinicio/login de la sección
  "Runbook MongoDB — diagnóstico, persistencia y reset". Confirmar ambos servicios healthy,
  salud de Auth UP y mismo usuario tras reinicio de Auth y tras `down`/`up` sin borrar volumen.
- H2.4 / TASK-38 AC #8: desde raíz, `.\mvnw.cmd -q -B verify` con Docker accesible;
  confirmar la suite completa y el gate JaCoCo. No se considera verificado en este entorno.

- H2.3: desde `services/auth-service`, sin variables JWT globales,
  ejecutar `./mvnw -q -B verify` en Linux o `.\mvnw.cmd -q -B clean verify` en Windows
  con Docker accesible. Confirmar GitHub Actions verde y revisar los reportes publicados.
- H2.3: ejecutar el arranque Compose indicado abajo y el smoke de registro/login/`/me`
  de este documento para verificar la imagen final (TASK-34 AC #3).
- Docker está instalado, pero `docker info` devuelve permiso denegado al daemon
  `dockerDesktopLinuxEngine`. Pendientes build, arranque y HTTP 200 (AC #8):
  desde `services/auth-service`, `docker compose --env-file .env.example up --build -d --wait`,
  `docker compose ps`, `Invoke-WebRequest http://localhost:8080/actuator/health`,
  `docker compose down`.
- Helm no está instalado. Pendientes `helm lint helm/auth-service` y ambos comandos
  `helm template` indicados arriba (AC #12); no se instaló ni descargó Helm.
- Ejecución remota de GitHub Actions, build Linux/Flapdoodle y despliegue en un cluster
  requieren esos entornos; los comandos Maven equivalentes pasaron localmente en Windows.
