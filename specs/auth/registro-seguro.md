---
title: Auth — Registro seguro + hashing (H2.2)
status: borrador
source: https://app.notion.com/p/H2-2-Registro-seguro-hashing-3ac75bb08ca781c3bd97d9b6197ae54c
updated: 2026-10-22T16:13:41.477Z
---

# Auth — Registro seguro + hashing

## Propósito

Implementar el caso de uso de registro de usuarios sobre MongoDB con validación de campos, normalización de email,
detección de duplicados, hashing seguro de contraseña y emisión de JWT. El sistema DEBE garantizar que ningún
dato sensible (password en texto plano) quede expuesto ni en la base ni en la respuesta HTTP.

## Requisitos

### Requisito: Validación de campos de registro

El sistema DEBE validar que los campos `name`, `email` y `password` estén presentes y cumplan las reglas mínimas
antes de intentar persistir el usuario.

#### Escenario: Registro con campos válidos

- **DADO** que un cliente envía `name` no vacío, `email` con formato válido y `password` de mínimo 8 caracteres
- **CUANDO** se invoca el endpoint de registro
- **ENTONCES** la solicitud pasa la capa de validación y continúa el flujo de creación

#### Escenario: Password demasiado corto

- **DADO** que un cliente envía un `password` con menos de 8 caracteres
- **CUANDO** se invoca el endpoint de registro
- **ENTONCES** el sistema rechaza la solicitud con error 400 (Bad Request)
- **Y** el mensaje de error indica que el password no cumple el mínimo de caracteres

#### Escenario: Email con formato inválido

- **DADO** que un cliente envía un `email` que no tiene formato de dirección de correo válida
- **CUANDO** se invoca el endpoint de registro
- **ENTONCES** el sistema rechaza la solicitud con error 400 (Bad Request)

#### Escenario: Campo obligatorio ausente

- **DADO** que un cliente envía la solicitud sin `name`, sin `email` o sin `password`
- **CUANDO** se invoca el endpoint de registro
- **ENTONCES** el sistema rechaza la solicitud con error 400 (Bad Request)
- **Y** el mensaje de error identifica el campo faltante

---

### Requisito: Normalización de email

El sistema DEBE normalizar el email recibido aplicando `trim` y conversión a minúsculas antes de cualquier
comparación de duplicados o persistencia.

#### Escenario: Email con mayúsculas y espacios

- **DADO** que un cliente envía el email `" Usuario@Ejemplo.COM "`
- **CUANDO** se procesa el registro
- **ENTONCES** el email almacenado y el usado para detectar duplicados es `usuario@ejemplo.com`

---

### Requisito: Detección de email duplicado

El sistema DEBE rechazar el registro cuando ya existe un usuario con el mismo email normalizado, devolviendo el
error 409 Conflict. DEBE manejar también la condición de carrera en la que dos solicitudes concurrentes intentan
registrar el mismo email simultáneamente.

#### Escenario: Email ya registrado (secuencial)

- **DADO** que ya existe un usuario con email `usuario@ejemplo.com`
- **CUANDO** se intenta registrar otro usuario con email `usuario@ejemplo.com` (o cualquier variante que
  normalice al mismo valor)
- **ENTONCES** el sistema devuelve HTTP 409 Conflict
- **Y** el cuerpo de respuesta sigue el contrato de error estándar del servicio
- **Y** el segundo usuario NO queda persistido

#### Escenario: Condición de carrera (índice único de MongoDB)

- **DADO** que dos solicitudes concurrentes intentan registrar el mismo email
- **CUANDO** la segunda solicitud llega antes de que la primera se haya confirmado en Mongo
- **CUANDO** MongoDB lanza `DuplicateKeyException` al intentar persistir
- **ENTONCES** el sistema captura esa excepción y devuelve HTTP 409 Conflict
- **Y** el cuerpo de respuesta sigue el contrato de error estándar del servicio

---

### Requisito: Hashing de password con BCrypt

El sistema DEBE hashear el password del usuario con BCrypt (o `PasswordEncoder` de Spring Security) antes de
persistirlo. NUNCA DEBE almacenar el password en texto plano en MongoDB.

#### Escenario: Password almacenado como hash

- **DADO** que el usuario envía el password `"miPassword123"`
- **CUANDO** se persiste el usuario
- **ENTONCES** el campo almacenado en MongoDB es un hash BCrypt (comienza con `$2a$` o `$2b$`)
- **Y** el valor original `"miPassword123"` no aparece en ningún campo del documento

---

### Requisito: Persistencia exitosa del usuario

El sistema DEBE persistir el nuevo usuario en MongoDB con todos los campos definidos por el modelo (ver
`auth/persistencia`) cuando la validación y la verificación de unicidad pasan.

#### Escenario: Registro exitoso

- **DADO** que los campos son válidos y el email no existe en la base
- **CUANDO** se completa el flujo de registro
- **ENTONCES** el documento del usuario queda guardado en MongoDB con `id`, `name`, `email` (normalizado),
  `passwordHash`, `createdAt`, `updatedAt`
- **Y** el campo `passwordHash` contiene el hash BCrypt, no el texto plano

---

### Requisito: Emisión de JWT tras registro exitoso

El sistema DEBE emitir un JWT válido y devolverlo al cliente como parte de la respuesta 201 Created, junto con
el perfil del usuario sin datos sensibles.

#### Escenario: Respuesta 201 con JWT y perfil

- **DADO** que el registro se completa exitosamente
- **CUANDO** el servicio responde al cliente
- **ENTONCES** el status HTTP es 201 Created
- **Y** el cuerpo contiene un JWT válido firmado
- **Y** el cuerpo contiene el perfil del usuario con al menos `id`, `name`, `email`
- **Y** el cuerpo NO contiene el campo `passwordHash` ni ningún otro dato sensible

---

### Requisito: Respuesta sin datos sensibles

El sistema NO DEBE incluir `passwordHash` ni ningún otro campo sensible del documento interno en ninguna
respuesta HTTP del endpoint de registro, ni en 201 ni en errores que puedan exponer datos internos.

#### Escenario: Campo passwordHash ausente en respuesta

- **DADO** un registro exitoso
- **CUANDO** el cliente recibe la respuesta 201
- **ENTONCES** el cuerpo de respuesta no contiene `passwordHash`
- **Y** no contiene ningún campo cuyo nombre sugiera password o hash

#### Escenario: Error no expone datos internos

- **DADO** un registro fallido (400 o 409)
- **CUANDO** el cliente recibe la respuesta de error
- **ENTONCES** el cuerpo sigue el contrato de error estándar y no expone datos del documento interno

## Preguntas abiertas

- **Algoritmo/configuración de JWT**: La especificación menciona "emitir JWT" pero no define el algoritmo de
  firma (HS256, RS256), el tiempo de expiración ni el payload exacto (claims). Se asume HS256 con expiración
  configurable por variable de entorno. El arquitecto confirma en el diseño técnico.
- **Nombre del endpoint**: La especificación no define el path HTTP exacto del endpoint de registro
  (ej. `POST /api/v1/auth/register` vs `POST /api/v1/users`). El arquitecto lo define en la especificación
  de arquitectura.
- **Fuerza de BCrypt**: La especificación no define el cost factor de BCrypt. Se asume el valor por defecto de
  Spring Security (10). El arquitecto puede ajustarlo si hay requisitos de rendimiento/seguridad.
