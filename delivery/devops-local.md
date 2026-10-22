# Operación local de auth-service

Requisitos: Java 21, Docker con Compose v2 o posterior; Helm 3 y kubectl para Kubernetes.
Persistencia: MongoDB 7. Identidad y puerto tomados del POM y de specs/auth/arquitectura.md.
Todos los comandos PowerShell parten de la raíz del repositorio, salvo donde se indica.

## Compose

```powershell
Set-Location services/auth-service
Copy-Item .env.example .env
docker compose config --quiet
docker compose up --build -d --wait
docker compose ps
Invoke-WebRequest http://localhost:8080/actuator/health
docker compose logs --tail 100 auth-service
docker compose down
Set-Location ../..
```

Editar `.env` antes de arrancar si se cambian la conexión o el puerto. El ejemplo no contiene
credenciales: `MONGO_URI=mongodb://mongodb:27017`, `MONGO_DATABASE=auth_local`, `PORT=8080`.
`PORT` publica el puerto del host; la app dentro de Compose escucha en 8080.
MongoDB conserva datos en el volumen `mongodb-data`; `down` no lo elimina.
MongoDB local no usa autenticación y su puerto se publica solo en loopback para desarrollo.
No usar este Compose como configuración de producción.
La app espera a que el ping de MongoDB tenga éxito y ambos servicios tienen healthcheck.

La imagen usa JDK 21 alpine para compilar con el wrapper y JRE 21 alpine para ejecutar el JAR.
El usuario tiene UID/GID 10001. MONGO_URI y MONGO_DATABASE no tienen conexión predeterminada.
El build de imagen omite tests; CI los ejecuta antes del build Docker.

## Maven y perfiles

```powershell
.\mvnw.cmd clean verify -B
.\mvnw.cmd dependency:tree -Dverbose -B
```

Linux/CI: `cd services/auth-service` y `./mvnw clean verify -B`.
No se versionan `target/`, reportes ni `.env`.
Los perfiles dev/docker/test/prod requieren MONGO_URI y MONGO_DATABASE por entorno, sin
fallback embebido. Solo los tests existentes crean MongoDB aislado mediante Flapdoodle y
DynamicPropertySource. El perfil test habilita índices, sin arrancar una base implícitamente.

App fuera de Docker (MongoDB de Compose en localhost):

```powershell
Set-Location services/auth-service
docker compose --env-file .env.example up -d mongodb
$env:MONGO_URI = 'mongodb://localhost:27017'
$env:MONGO_DATABASE = 'auth_local'
$env:SPRING_PROFILES_ACTIVE = 'dev'
.\mvnw.cmd spring-boot:run
# Tras detener la app con Ctrl+C:
Remove-Item Env:MONGO_URI, Env:MONGO_DATABASE, Env:SPRING_PROFILES_ACTIVE
docker compose down
Set-Location ../..
```

Dev y docker crean índices automáticamente. Prod mantiene auto-index-creation desactivado.
Antes de habilitar tráfico en producción, un operador debe crear el índice único con las
variables de conexión del entorno y permisos de administración de índices:

```powershell
mongosh $env:MONGO_URI --quiet --eval 'db.getSiblingDB(process.env.MONGO_DATABASE).users.createIndex({email: 1}, {unique: true})'
```

## CI/CD

`.github/workflows/auth-service.yml` verifica cada push/PR que afecte el servicio, su workflow
o el chart, con Temurin 21, caché Maven y `./mvnw clean verify`. En push a main construye la
imagen con tag `ghcr.io/<org>/<repo>:<sha>`, tras pasar tests. No publica imágenes porque no
hay registry autorizado/configurado. Para push futuro: configurar permisos packages:write
y GITHUB_TOKEN, o credencial del registry en GitHub Secrets. No hay secretos en workflows.
`auth-service-cd.yml` es una plantilla manual informativa; no despliega. Configurar registry,
cluster, credenciales y environment protegido antes de agregar un deploy real.

## Helm

MongoDB es externo al chart. Por defecto se referencia el Secret `auth-mongodb` con la clave
`MONGO_URI`; aprovisionarlo mediante el gestor de secretos del entorno. Alternativamente,
vaciar `mongo.existingSecret` y pasar `mongo.uri` desde un archivo privado ignorado por Git
(por ejemplo `services/auth-service/.env.helm.yaml`), nunca desde valores versionados.
La base se configura con `mongo.database`; imagen, tag y imagePullSecrets son configurables.
Los valores por defecto permiten renderizar sin secretos reales; no crean el Secret externo.

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
crear el índice y aprovisionar el Secret antes del despliegue. No se realizó deploy real.

## Verificación realizada

- `mvnw.cmd clean verify -B -q`: pasó con Java 21 y gate JaCoCo 80%.
- `mvnw.cmd dependency:tree -Dverbose -B`: BUILD SUCCESS.
- `docker compose --env-file .env.example config --quiet`: pasó.
- Sintaxis YAML validada con SnakeYAML 2.4 ya presente en el caché Maven: workflows,
  perfiles, Compose, Chart.yaml y values de Helm; rechazo de claves duplicadas habilitado.
  Las plantillas Go de Helm requieren Helm para su validación completa.
- `git diff --check`: pasó. Los archivos entregados solo contienen ejemplos sin credenciales.

## Pendiente de verificar

- Docker está instalado, pero `docker info` devuelve permiso denegado al daemon
  `dockerDesktopLinuxEngine`. Pendientes build, arranque y HTTP 200 (AC #8):
  desde `services/auth-service`, `docker compose --env-file .env.example up --build -d --wait`,
  `docker compose ps`, `Invoke-WebRequest http://localhost:8080/actuator/health`,
  `docker compose down`.
- Helm no está instalado. Pendientes `helm lint helm/auth-service` y ambos comandos
  `helm template` indicados arriba (AC #12); no se instaló ni descargó Helm.
- Ejecución remota de GitHub Actions, build Linux/Flapdoodle y despliegue en un cluster
  requieren esos entornos; los comandos Maven equivalentes pasaron localmente en Windows.
