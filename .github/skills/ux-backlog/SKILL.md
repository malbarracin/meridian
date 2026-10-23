---
name: ux-backlog
description: Parte el trabajo de diseño en tareas verificables (Design System, pantallas por canal, estados, handoff) en el tablero.
---

# Tablero del equipo de UX

## Tareas (con `task_create`, etiquetas `run:<id>` y `role:<rol del diseñador>`)

1. **Design System**: archivo de Penpot del proyecto, variables (color, tipografía, espacio, radios), componentes base
   con variantes y estados. Va primero: las demás dependen de esta.
2. **Una tarea por pantalla y canal** (ej. "Login — web", "Login — mobile", "Dashboard — web"…), que dependa del
   Design System.
3. **Estados y motion** de las pantallas con datos (cargando, error parcial, error total, reduced-motion), si no
   entran en las tareas de cada pantalla.
4. El **handoff** lo cierra el documentador: no lleva tarea propia salvo que el equipo no tenga ese rol.

## Criterios de aceptación (copiar en cada tarea)

- El frame existe en Penpot con el nombre `<Pantalla> / <canal>` y su link está en `ux/handoff.md`.
- Usa solo variables y componentes del Design System.
- Cumple las restricciones de marca y contenido de `specs/ux/` (textuales).
- Tiene sus estados documentados.

## Checklist

- [ ] El Design System es la primera tarea y las pantallas dependen de ella
- [ ] Hay una tarea por pantalla y por canal de la especificación
- [ ] Cada tarea lleva las restricciones de marca en sus criterios
