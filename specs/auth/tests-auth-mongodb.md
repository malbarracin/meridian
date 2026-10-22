---
title: Auth — Tests de integración con MongoDB (H2.3)
status: borrador
source: https://app.notion.com/p/H2-3-Tests-Auth-con-MongoDB-3ac75bb08ca7819ea1bfe51ac5132d15
updated: 2026-10-22T17:56:59.694Z
---

# Auth — Tests de integración con MongoDB

## Propósito

Validar el comportamiento del servicio de autenticación (`services/auth-service`) contra una instancia real de
MongoDB provista por Testcontainers. El objetivo es garantizar que los flujos de registro, login y consulta de
perfil funcionan de extremo a extremo sobre una base de datos real (no en memoria), y que el build pasa el gate
de cobertura JaCoCo LINE ≥ 70%.

Este hito cubre los hitos previos H2.1 (persistencia MongoDB) y H2.2 (registro seguro + JWT), por lo que los
tests DEBEN ejercitar la integración completa de esas capas (repository, service, controller).

## Requisitos

### Requisito: Configuración de Testcontainers para MongoDB

El sistema DEBE proveer una configuración de Testcontainers que levante un contenedor MongoDB efímero para la
ejecución de los tests de integración, sin depender de ninguna instancia MongoDB externa ni de bases de datos
embebidas en memoria.

#### Escenario: Levantamiento de contenedor efímero

- **DADO** que se ejecuta el build (`mvn clean verify`)
- **CUANDO** se inician los tests de integración
- **ENTONCES** Testcontainers levanta un contenedor MongoDB real y los tests se conectan a él automáticamente
- **Y** el contenedor se destruye al finalizar la suite de tests

#### Escenario: Aislamiento entre tests

- **DADO** que varios tests de integración comparten el mismo contenedor
- **CUANDO** cada test escribe o modifica datos
- **ENTONCES** cada test parte de un estado conocido (limpio o con seed explícito) sin interferir con otros

---

### Requisito: Test de registro persiste usuario

El sistema DEBE verificar que el flujo de registro persiste correctamente el documento del usuario en MongoDB
con todos los campos del modelo.

#### Escenario: Register persiste usuario en MongoDB

- **DADO** un contenedor MongoDB limpio
- **CUANDO** se invoca el endpoint `POST /api/v1/users` con `name`, `email` y `password` válidos
- **ENTONCES** el test verifica que el documento queda en MongoDB con `id`, `name`, `email` (normalizado),
  `passwordHash`, `createdAt`, `updatedAt`
- **Y** el campo `passwordHash` contiene un hash BCrypt (comienza con `$2a$` o `$2b$`) y no el texto plano

---

### Requisito: Test de email duplicado devuelve 409

El sistema DEBE verificar que un segundo intento de registro con el mismo email normalizado es rechazado con
HTTP 409, incluso cuando el segundo email difiere en mayúsculas/minúsculas.

#### Escenario: Email duplicado exacto → 409

- **DADO** que ya existe un usuario con email `usuario@ejemplo.com` en el contenedor
- **CUANDO** se intenta registrar otro usuario con el mismo email `usuario@ejemplo.com`
- **ENTONCES** el sistema responde HTTP 409 Conflict
- **Y** el cuerpo sigue el contrato de error estándar del servicio
- **Y** el segundo documento NO queda persistido en MongoDB

#### Escenario: Email duplicado con mayúsculas → 409

- **DADO** que ya existe un usuario con email `usuario@ejemplo.com` en el contenedor
- **CUANDO** se intenta registrar otro usuario con email `Usuario@Ejemplo.COM`
- **ENTONCES** el sistema responde HTTP 409 Conflict
- **Y** el segundo documento NO queda persistido en MongoDB

---

### Requisito: Test de password almacenado como hash

El sistema DEBE verificar que el password nunca se almacena en texto plano en MongoDB: el campo del documento
DEBE ser un hash BCrypt.

#### Escenario: Password guardado como hash

- **DADO** que se registra un usuario con password `\"miPassword123\"`
- **CUANDO** se consulta directamente el documento en MongoDB
- **ENTONCES** el campo `passwordHash` contiene un valor que comienza con `$2a$` o `$2b$`
- **Y** el valor original `\"miPassword123\"` no aparece en ningún campo del documento

---

### Requisito: Test de login correcto tras registro

El sistema DEBE verificar que un usuario recién registrado puede autenticarse correctamente (login exitoso).

#### Escenario: Login correcto tras registro

- **DADO** que un usuario se registró con email `usuario@ejemplo.com` y password `\"miPassword123\"`
- **CUANDO** se invoca el endpoint de login con las mismas credenciales
- **ENTONCES** el sistema responde con HTTP 200 y un JWT válido en el cuerpo
- **Y** el JWT puede decodificarse y contiene el identificador del usuario

---

### Requisito: Test de login incorrecto devuelve 401

El sistema DEBE verificar que un intento de login con credenciales incorrectas (password erróneo o email
inexistente) es rechazado con HTTP 401.

#### Escenario: Password incorrecto → 401

- **DADO** que existe un usuario registrado con email `usuario@ejemplo.com`
- **CUANDO** se intenta hacer login con ese email y un password diferente al registrado
- **ENTONCES** el sistema responde HTTP 401 Unauthorized

#### Escenario: Email inexistente → 401

- **DADO** que no existe ningún usuario con email `noexiste@ejemplo.com`
- **CUANDO** se intenta hacer login con ese email
- **ENTONCES** el sistema responde HTTP 401 Unauthorized

---

### Requisito: Test de consulta de perfil (`/me`)

El sistema DEBE verificar que el endpoint `/me` (o equivalente) devuelve el perfil del usuario autenticado tal
como fue persistido en MongoDB, sin exponer datos sensibles.

#### Escenario: /me lee perfil persistido

- **DADO** que un usuario se registró y obtuvo un JWT válido
- **CUANDO** se invoca el endpoint `/me` con ese JWT en el header `Authorization: Bearer <token>`
- **ENTONCES** el sistema responde HTTP 200 con el perfil del usuario (`id`, `name`, `email`)
- **Y** la respuesta NO contiene `passwordHash` ni ningún otro dato sensible
- **Y** los valores coinciden con lo persistido en MongoDB

---

### Requisito: Test de seed idempotente

El sistema DEBE verificar que la carga inicial de datos (seed) es idempotente: ejecutarla más de una vez no
genera duplicados ni errores.

#### Escenario: Seed ejecutado dos veces sin errores

- **DADO** que el seed de datos iniciales se ejecuta sobre el contenedor MongoDB
- **CUANDO** el mismo seed se ejecuta por segunda vez
- **ENTONCES** no se lanzan excepciones
- **Y** los datos en MongoDB no se duplican

---

### Requisito: Test de persistencia disponible tras reinicio de conexión

El sistema DEBE verificar que los datos persisten y son accesibles luego de que la conexión MongoDB se
interrumpe y se restablece (resiliencia básica de persistencia).

#### Escenario: Persistencia disponible tras reinicio de conexión

- **DADO** que un usuario fue persistido en MongoDB
- **CUANDO** la conexión al contenedor se interrumpe y se restablece
- **ENTONCES** el documento del usuario sigue disponible en MongoDB
- **Y** las operaciones de lectura funcionan correctamente tras el restablecimiento

---

### Requisito: Gate de cobertura JaCoCo LINE ≥ 70%

El sistema DEBE cumplir un umbral mínimo de cobertura de líneas del **70%** medido por JaCoCo al ejecutar
`mvn clean verify`. El build DEBE fallar si no se alcanza ese umbral.

#### Escenario: Build pasa con cobertura suficiente

- **DADO** que los tests de integración y unitarios cubren las capas repository, service y controller
- **CUANDO** se ejecuta `mvn clean verify`
- **ENTONCES** el reporte JaCoCo muestra LINE coverage ≥ 70%
- **Y** el build termina con éxito (exit code 0)

#### Escenario: Build falla con cobertura insuficiente

- **DADO** que la cobertura de líneas es inferior al 70%
- **CUANDO** se ejecuta `mvn clean verify`
- **ENTONCES** el paso `check` de JaCoCo falla con un error descriptivo
- **Y** el build termina con código de salida distinto de cero

---

### Requisito: Cobertura de capas repository, service y controller

El sistema DEBE tener tests que ejerciten al menos las tres capas de la arquitectura del `auth-service`:
repository (integración con MongoDB), service (lógica de negocio) y controller (contratos HTTP).

#### Escenario: Tests de repository contra MongoDB real

- **DADO** un contenedor MongoDB levantado por Testcontainers
- **CUANDO** se ejecutan los tests de repository
- **ENTONCES** se verifican operaciones de lectura y escritura reales (no mocks de la base de datos)

#### Escenario: Tests de service con lógica de negocio

- **DADO** una instancia del service con sus dependencias
- **CUANDO** se ejecutan los tests de service
- **ENTONCES** se verifican las reglas de negocio (validación, normalización, hashing, JWT) con o sin mock de MongoDB según corresponda

#### Escenario: Tests de controller con contratos HTTP

- **DADO** el contexto Spring levantado con Testcontainers
- **CUANDO** se ejecutan los tests de controller
- **ENTONCES** se verifican los contratos HTTP (status codes, estructura del body, headers) de todos los endpoints de autenticación

## Preguntas abiertas

- **Endpoint de login**: La especificación H2.3 menciona \"login correcto tras registro\" pero los hitos H2.1 y H2.2 no definen el endpoint de login ni su contrato. Se asume que existe o existirá un endpoint de login (probablemente `POST /api/v1/auth/login` o equivalente). El arquitecto confirma el path y el contrato en el diseño técnico.
- **Endpoint `/me`**: La especificación menciona `/me` como endpoint para consultar el perfil del usuario autenticado. El path exacto y el mecanismo de autenticación (JWT Bearer) no están definidos en H2.1/H2.2. Se asume `/api/v1/users/me` con Bearer token. El arquitecto confirma.
- **Umbral JaCoCo**: La especificación H2.3 indica JaCoCo LINE ≥ 70%, que es inferior al 80% definido en el stack pack. Se respeta el umbral de la especificación (70%) para este hito; el arquitecto puede decidir elevar al 80% si el stack pack es más restrictivo.
- **Resiliencia de conexión**: El caso \"persistencia disponible tras reinicio de conexión\" no especifica el mecanismo concreto (pausa del contenedor, cierre de socket, etc.). Se deja como requisito de comportamiento observable; el arquitecto/dev define el mecanismo de test.
