---
name: ux-handoff
description: Handoff para web y mobile en ux/handoff.md — tablero de cada pantalla (nombre y link), tokens, componentes, estados, microcopy, assets y motion.
---

# Handoff (`ux/handoff.md`)

Usá la plantilla `ux-handoff` y la regla `handoff-format.md`. Es lo **único** que el equipo de front lee para saber qué
construir y dónde está cada diseño.

## Por cada pantalla y canal

- **Tablero**: nombre exacto (`<Pantalla> / <canal>`) y link al archivo (en Figma, con su `node-id`).
  El front lo trae con el MCP de diseño (skill `design-to-code`).
- Componentes que usa y sus variantes.
- Estados (link a cada frame de estado).
- Microcopy: títulos, botones, errores (textuales).
- Datos que muestra y de qué campo de la API sale cada uno (si se conoce el contrato).
- Assets a exportar (logo, íconos) y en qué formato.
- Motion (qué, duración, curva, reduced-motion).

## Checklist

- [ ] Cada pantalla de cada canal tiene su tablero (nombre y link)
- [ ] El microcopy está textual
- [ ] Cada estado tiene su frame
