---
title: Origen de los requisitos — Meridian
status: borrador
source: https://app.notion.com/p/3ab75bb08ca781e7b24ae01f9cffc0e7
updated: 2026-10-22T20:07:32.607Z
---

# Origen de los requisitos

| Campo | Valor |
|-------|-------|
| Proyecto | Meridian |
| Fuente | Notion |
| Link raíz | https://app.notion.com/p/3ab75bb08ca781e7b24ae01f9cffc0e7 |
| Actualizado | 2026-10-22 |

## Requisito → fuente

| Requisito | Área | Id en la fuente |
|-----------|------|-----------------|
| Documento User | auth/persistencia | H2.1 — Persistencia MongoDB (Auth) — Sección 2. Alcance / Sección 3. CA |
| Unicidad de email | auth/persistencia | H2.1 — Sección 2. Alcance ("Índice único sobre email normalizado") / Sección 3. CA ("Email duplicado bloqueado") |
| UserRepository | auth/persistencia | H2.1 — Sección 2. Alcance ("UserRepository con búsqueda por email normalizado") / Sección 3. CA ("Repository separado de service/controller") |
| Configuración por variables de entorno (persistencia) | auth/persistencia | H2.1 — Sección 2. Alcance ("Mongo URI y database por variables de entorno", "Sin fallback silencioso in-memory") / Sección 3. CA ("Secrets no hardcodeados") |
| Dependencia Spring Data MongoDB | auth/persistencia | H2.1 — Sección 2. Alcance ("Dependencia Spring Data MongoDB") |
| Validación de campos de registro | auth/registro-seguro | H2.2 — Sección 2. Alcance ("Validar nombre, email y password (mín. 8 caracteres)") / Sección 3. CA |
| Normalización de email | auth/registro-seguro | H2.2 — Sección 2. Alcance ("Normalizar email (trim + lowercase)") |
| Detección de email duplicado | auth/registro-seguro | H2.2 — Sección 2. Alcance ("Detectar duplicado → 409", "Manejar condición de carrera del índice único") / Sección 3. CA ("Email duplicado 409") |
| Hashing de password con BCrypt | auth/registro-seguro | H2.2 — Sección 2. Alcance ("Hashear password con BCrypt/PasswordEncoder") / Sección 3. CA ("Password solo como hash") |
| Persistencia exitosa del usuario | auth/registro-seguro | H2.2 — Sección 2. Alcance ("Persistir usuario en MongoDB") |
| Emisión de JWT tras registro exitoso | auth/registro-seguro | H2.2 — Sección 2. Alcance ("Emitir JWT y devolver perfil sin datos sensibles") / Sección 3. CA ("Register 201 + JWT") |
| Respuesta sin datos sensibles | auth/registro-seguro | H2.2 — Sección 3. CA ("Respuesta sin passwordHash") |
| Configuración de Testcontainers para MongoDB | auth/tests-auth-mongodb | H2.3 — Sección 3. CA ("Testcontainers Mongo configurado") |
| Test de registro persiste usuario | auth/tests-auth-mongodb | H2.3 — Sección 2. Casos mínimos ("Register persiste usuario") |
| Test de email duplicado devuelve 409 | auth/tests-auth-mongodb | H2.3 — Sección 2. Casos mínimos ("Email duplicado (incluyendo mayúsculas) → 409") |
| Test de password almacenado como hash | auth/tests-auth-mongodb | H2.3 — Sección 2. Casos mínimos ("Password guardado como hash") |
| Test de login correcto tras registro | auth/tests-auth-mongodb | H2.3 — Sección 2. Casos mínimos ("Login correcto tras registro") |
| Test de login incorrecto devuelve 401 | auth/tests-auth-mongodb | H2.3 — Sección 2. Casos mínimos ("Login incorrecto → 401") |
| Test de consulta de perfil (/me) | auth/tests-auth-mongodb | H2.3 — Sección 2. Casos mínimos ("/me lee perfil persistido") |
| Test de seed idempotente | auth/tests-auth-mongodb | H2.3 — Sección 2. Casos mínimos ("Seed idempotente") |
| Test de persistencia tras reinicio de conexión | auth/tests-auth-mongodb | H2.3 — Sección 2. Casos mínimos ("Persistencia disponible tras reinicio de conexión") |
| Gate de cobertura JaCoCo LINE ≥ 70% | auth/tests-auth-mongodb | H2.3 — Sección 3. CA ("JaCoCo LINE ≥ 70%") |
| Cobertura de capas repository, service y controller | auth/tests-auth-mongodb | H2.3 — Sección 3. CA ("Tests repository/service/controller") |
| Servicio MongoDB en Compose | infra/mongodb-local | H2.4 — Sección 2. Alcance ("Servicio MongoDB en Compose", "Volumen persistente", "Healthcheck") |
| Auth Service espera a MongoDB saludable | infra/mongodb-local | H2.4 — Sección 2. Alcance ("Auth espera estado saludable") / Sección 3. CA ("Compose inicia MongoDB + Auth", "Auth health OK con Mongo disponible") |
| Configuración por variables de entorno (Compose) | infra/mongodb-local | H2.4 — Sección 2. Alcance ("Variables MONGODB_URI / database / credenciales vía entorno") / Sección 3. CA ("Secrets no versionados") |
| Persistencia de datos de demo | infra/mongodb-local | H2.4 — Sección 3. CA ("Usuario registrado sobrevive al reinicio de Auth") |
| Runbook de diagnóstico y reset | infra/mongodb-local | H2.4 — Sección 2. Alcance ("Documentar reset de datos de demo") / Sección 3. CA ("Runbook con diagnóstico y reset") |

## Fuentes visitadas

| Página Notion | Título | Rol |
|---------------|--------|-----|
| https://app.notion.com/p/3ac75bb08ca7819bbb50ec4b5a5a8840 | 🍃 H2.1 — Persistencia MongoDB (Auth) | Fuente principal de auth/persistencia |
| https://app.notion.com/p/3ac75bb08ca781c3bd97d9b6197ae54c | 🔒 H2.2 — Registro seguro + hashing | Fuente principal de auth/registro-seguro |
| https://app.notion.com/p/3ac75bb08ca7819ea1bfe51ac5132d15 | 🧪 H2.3 — Tests Auth con MongoDB | Fuente principal de auth/tests-auth-mongodb |
| https://app.notion.com/p/3ac75bb08ca781a68ee5d18db3da7d71 | 🐳 H2.4 — MongoDB en entorno local | Fuente principal de infra/mongodb-local |
| https://app.notion.com/p/3ab75bb08ca781e7b24ae01f9cffc0e7 | 🚀 Meridian — Requisitos de producto (Login + Dashboard mercados) | Contexto de producto y alcance global |
