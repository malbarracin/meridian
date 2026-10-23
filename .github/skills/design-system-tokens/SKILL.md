---
name: design-system-tokens
description: Design System en Penpot (variables y componentes) y su versión en texto (ux/design-system.md) para que web y mobile usen los mismos tokens.
---

# Design System y tokens

## Variables (colección "Tokens")

| Grupo | Ejemplos de nombres |
|---|---|
| color | `color/bg`, `color/surface`, `color/text`, `color/text-muted`, `color/primary`, `color/positive`, `color/negative`, `color/border` |
| tipografía | `font/display`, `font/title`, `font/body`, `font/caption` (familia, tamaño, peso, alto de línea) |
| espacio | `space/1` (4) … `space/8` (32) |
| radio | `radius/sm`, `radius/md`, `radius/lg` |
| motion | `motion/fast` (150 ms), `motion/base` (250 ms), easing |

Modo claro y oscuro si la especificación lo pide (mismo nombre, otro valor por modo).

## Componentes

Cada componente con **variantes y estados** (default, hover, focus, disabled, loading, error). Los que la
especificación nombre (ej. `MetricCard`, `ChartPanel`, `NewsCard`) llevan **ese** nombre.

## En texto: `ux/design-system.md`

Tabla de cada variable (nombre → valor por modo) y de cada componente (variantes, estados, tokens que usa). Es lo que
el front convierte en CSS/tema sin depender solo de Penpot.

## Checklist

- [ ] Todas las pantallas usan variables, no valores sueltos
- [ ] Los componentes nombrados en la especificación existen con ese nombre
- [ ] ux/design-system.md tiene los mismos tokens que Penpot
