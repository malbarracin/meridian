---
name: front-backlog
description: Parte el trabajo de front en tareas verificables (base, tokens, cliente de API, pantallas con su tablero de Penpot, estados, pruebas).
---

# Tablero de front

## Orden de las tareas (con `task_create` y dependencias)

1. **Base del proyecto**: crear la app en su carpeta, scripts (incluido `verify`), lint, pruebas.
2. **Tokens y tema** desde el Design System.
3. **Componentes del Design System** (los que nombra el handoff).
4. **Cliente de API y sesión** (login, 401, logout).
5. **Una tarea por pantalla** del canal, con el **tablero** de Penpot en la descripción.
6. **Estados** (cargando, vacío, error parcial, error total, reduced-motion) si no entran en cada pantalla.

## Criterios de aceptación (en cada pantalla)

- Se ve como el tablero de Penpot (link) usando tokens y componentes, sin valores sueltos.
- Maneja sus estados y los errores de la API.
- Textos tal cual el handoff (marca y microcopy).
- Pruebas y `verify` en verde.

## Checklist

- [ ] La base y los tokens van primero; las pantallas dependen de ellas
- [ ] Cada pantalla tiene su link de Penpot
- [ ] Ninguna tarea mezcla dos pantallas
