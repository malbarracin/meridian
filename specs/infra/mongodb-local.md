---
title: Infra — MongoDB en entorno local (H2.4)
status: aprobada
source: https://app.notion.com/p/3ac75bb08ca781a68ee5d18db3da7d71
updated: 2026-10-22T20:07:51.767Z
---

# Infra — MongoDB en entorno local (H2.4)

## Propósito

Dejar MongoDB disponible en el entorno de desarrollo/demo local mediante Docker Compose,
con volumen persistente, healthcheck, y conexión al Auth Service vía variables de entorno.
El objetivo es que cualquier desarrollador pueda levantar el stack completo con un solo
comando y que los datos de demo sobrevivan reinicios del servicio.

## Requisitos

### Requisito: Servicio MongoDB en Compose

El sistema DEBE definir un servicio MongoDB en `docker-compose.yml` con imagen oficial,
volumen persistente y healthcheck.

#### Escenario: Compose levanta MongoDB correctamente

- **DADO** que el desarrollador ejecuta `docker compose up`
- **CUANDO** Docker Compose inicializa el stack
- **ENTONCES** el servicio MongoDB arranca y queda en estado saludable (healthy)
- **Y** los datos persisten en un volumen nombrado (no bind mount efímero)

#### Escenario: MongoDB vuelve a healthy tras reinicio del contenedor

- **DADO** que el contenedor de MongoDB fue detenido y relanzado
- **CUANDO** el healthcheck confirma que MongoDB responde
- **ENTONCES** el servicio queda en estado `healthy` antes de que Auth inicie

---

### Requisito: Auth Service espera a MongoDB saludable

El sistema DEBE configurar el servicio Auth en Compose con `depends_on` condicionado al
estado `healthy` de MongoDB, de modo que Auth no inicie hasta que Mongo esté listo.

#### Escenario: Startup ordenado

- **DADO** que ambos servicios están definidos en Compose
- **CUANDO** se ejecuta `docker compose up`
- **ENTONCES** Auth Service no arranca hasta que MongoDB reporte estado `healthy`

#### Escenario: Auth health OK con Mongo disponible

- **DADO** que el stack completo está levantado
- **CUANDO** se consulta el endpoint `GET /actuator/health` de Auth Service
- **ENTONCES** la respuesta incluye estado `UP` y el indicador de MongoDB en `UP`

---

### Requisito: Configuración por variables de entorno

El sistema DEBE inyectar `MONGODB_URI`, el nombre de la base de datos y las credenciales
mediante variables de entorno; no se permiten valores de conexión hardcodeados en el código
ni en archivos versionados.

#### Escenario: Arranque con variables de entorno provistas

- **DADO** que las variables `MONGODB_URI` (o equivalentes `MONGO_HOST`/`MONGO_PORT`),
  `MONGO_DB`, `MONGO_USER` y `MONGO_PASSWORD` están definidas en `.env` (no versionado)
- **CUANDO** Compose arranca los servicios
- **ENTONCES** Auth conecta a MongoDB usando esos valores sin fallback silencioso a otra URI

#### Escenario: Secrets no versionados

- **DADO** que el repositorio está inicializado
- **CUANDO** se revisa el historial de git
- **ENTONCES** ningún archivo versionado contiene credenciales ni URIs de conexión con contraseña

---

### Requisito: Persistencia de datos de demo

El sistema DEBE garantizar que los datos de usuario registrados sobrevivan a un reinicio
del Auth Service (sin reiniciar MongoDB).

#### Escenario: Usuario registrado sobrevive reinicio de Auth

- **DADO** que un usuario fue registrado exitosamente con el stack levantado
- **CUANDO** el contenedor de Auth Service se reinicia (`docker compose restart auth-service`)
- **ENTONCES** el usuario puede iniciar sesión con las mismas credenciales

#### Escenario: Volumen persiste tras detención total del stack

- **DADO** que el stack fue detenido con `docker compose down` (sin `--volumes`)
- **CUANDO** se vuelve a levantar con `docker compose up`
- **ENTONCES** los datos del volumen de MongoDB siguen disponibles

---

### Requisito: Runbook de diagnóstico y reset

El sistema DEBE incluir documentación operativa (runbook) con los comandos para
verificar el estado del stack, diagnosticar problemas de conexión y resetear los datos
de demo a estado inicial.

#### Escenario: Desarrollador puede resetear datos de demo

- **DADO** que existe un runbook en la documentación del proyecto
- **CUANDO** el desarrollador necesita volver al estado inicial de datos de demo
- **ENTONCES** puede ejecutar los comandos del runbook para borrar el volumen y reiniciar el stack

#### Escenario: Desarrollador puede diagnosticar falla de conexión

- **DADO** que Auth Service no conecta a MongoDB
- **CUANDO** el desarrollador consulta el runbook
- **ENTONCES** encuentra pasos de diagnóstico (verificar estado del contenedor, logs, variables de entorno)

---

## Fuera de alcance

- Configuración de MongoDB para entornos productivos o staging
- Autenticación MongoDB con TLS/SSL
- Replica sets o sharding
- Persistencia productiva o multi-tenant
- Cambios en el código de Auth Service más allá de la configuración de conexión

## Preguntas abiertas

*(Ninguna — todos los requisitos de H2.4 están definidos en la fuente.)*
