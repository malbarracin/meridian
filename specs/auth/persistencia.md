---
title: Auth — Persistencia MongoDB (H2.1)
status: borrador
source: https://app.notion.com/p/H2-1-Persistencia-MongoDB-Auth-3ac75bb08ca7819bbb50ec4b5a5a8840
updated: 2026-10-22T14:28:36.218Z
---

# Auth — Persistencia MongoDB

## Propósito

Persistir usuarios en MongoDB para el servicio de autenticación (`services/auth-service`).
El sistema DEBE garantizar unicidad de email, que el password nunca se almacene en texto plano, y que la conexión a Mongo se configure exclusivamente por variables de entorno, sin fallback in-memory.

## Requisitos

### Requisito: Documento User

El sistema DEBE representar cada usuario como un documento MongoDB con los campos: `id`, `name`, `email` (normalizado), `passwordHash`, `createdAt`, `updatedAt`.

#### Escenario: Alta de usuario nuevo

- **DADO** que no existe un usuario con ese email en la base
- **CUANDO** se persiste un nuevo usuario con name, email válido y passwordHash
- **ENTONCES** el documento queda guardado en MongoDB con todos los campos (`id`, `name`, `email`, `passwordHash`, `createdAt`, `updatedAt`)
- **Y** el campo `email` se almacena en minúsculas sin espacios (`trim + lowercase`)

#### Escenario: Password no expuesto

- **DADO** un usuario recién creado
- **CUANDO** se consulta su documento en MongoDB
- **ENTONCES** el documento NO contiene el password en texto plano
- **Y** solo contiene `passwordHash` (hash del password)

---

### Requisito: Unicidad de email

El sistema DEBE rechazar el alta de un segundo usuario con el mismo email normalizado mediante un índice único en MongoDB.

#### Escenario: Email duplicado bloqueado

- **DADO** que ya existe un usuario con email `usuario@ejemplo.com`
- **CUANDO** se intenta guardar otro usuario con email `Usuario@Ejemplo.com` (o cualquier variante de mayúsculas/espacios que normalice al mismo valor)
- **ENTONCES** la operación falla con un error de clave duplicada
- **Y** el segundo documento NO se persiste en la base

---

### Requisito: UserRepository

El sistema DEBE exponer un repositorio (`UserRepository`) con al menos la operación de búsqueda por email normalizado, separado de la capa de servicio y de controlador.

#### Escenario: Búsqueda por email

- **DADO** un usuario guardado con email `usuario@ejemplo.com`
- **CUANDO** se invoca la búsqueda por email con el valor `usuario@ejemplo.com`
- **ENTONCES** el repositorio devuelve el documento del usuario correspondiente

#### Escenario: Email no encontrado

- **DADO** que no existe ningún usuario con el email consultado
- **CUANDO** se invoca la búsqueda por ese email
- **ENTONCES** el repositorio devuelve un resultado vacío (sin lanzar excepción)

---

### Requisito: Configuración por variables de entorno

El sistema DEBE leer la URI de MongoDB y el nombre de la base de datos exclusivamente desde variables de entorno; NO DEBE tener valores de conexión hardcodeados ni usar base de datos embebida como fallback silencioso en ningún perfil.

#### Escenario: Variables presentes

- **DADO** que las variables de entorno `MONGO_URI` y `MONGO_DATABASE` (o equivalentes del proyecto) están definidas
- **CUANDO** el servicio arranca
- **ENTONCES** se conecta a la instancia MongoDB indicada y las operaciones de lectura/escritura funcionan correctamente

#### Escenario: Variables ausentes

- **DADO** que las variables de entorno de conexión NO están definidas
- **CUANDO** el servicio intenta arrancar
- **ENTONCES** el arranque falla con un error explícito (no cae silenciosamente en H2 in-memory)

---

### Requisito: Dependencia Spring Data MongoDB

El sistema DEBE incluir la dependencia `spring-boot-starter-data-mongodb` en el POM del `auth-service` para habilitar la integración con MongoDB.

#### Escenario: Compilación con dependencia

- **DADO** que el POM del `auth-service` incluye `spring-boot-starter-data-mongodb`
- **CUANDO** se ejecuta el build (`./mvnw clean verify`)
- **ENTONCES** el proyecto compila sin errores y los tests de integración con MongoDB levantan correctamente

## Preguntas abiertas

- **Nombre de las variables de entorno**: La especificación no define los nombres exactos de las variables (ej. `SPRING_DATA_MONGODB_URI` vs `MONGO_URI`). Se asume convención Spring Boot estándar (`spring.data.mongodb.uri` / `spring.data.mongodb.database`) configurable vía variables de entorno. El arquitecto confirma en el diseño técnico.
- **Índice de email**: La especificación indica índice único sobre email normalizado. Se asume que la normalización (`trim + lowercase`) ocurre antes de persistir (en la capa de servicio o en un hook del documento). El arquitecto define el punto exacto.
