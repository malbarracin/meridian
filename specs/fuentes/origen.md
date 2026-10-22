---
title: Origen de los requisitos — Meridian
status: aprobada
source: https://app.notion.com/p/3ab75bb08ca781e7b24ae01f9cffc0e7
updated: 2026-10-22T14:30:32.559Z
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
| Configuración por variables de entorno | auth/persistencia | H2.1 — Sección 2. Alcance ("Mongo URI y database por variables de entorno", "Sin fallback silencioso in-memory") / Sección 3. CA ("Secrets no hardcodeados") |
| Dependencia Spring Data MongoDB | auth/persistencia | H2.1 — Sección 2. Alcance ("Dependencia Spring Data MongoDB") |

## Fuentes visitadas

| Página Notion | Título | Rol |
|---------------|--------|-----|
| https://app.notion.com/p/3ac75bb08ca7819bbb50ec4b5a5a8840 | 🍃 H2.1 — Persistencia MongoDB (Auth) | Fuente principal de esta spec |
| https://app.notion.com/p/3ab75bb08ca781e7b24ae01f9cffc0e7 | 🚀 Meridian — Requisitos de producto (Login + Dashboard mercados) | Contexto de producto y alcance global |
