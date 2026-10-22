---
name: ux-states-motion
description: Estados de cada pantalla (cargando, vacío, error parcial, error total, sin sesión) y motion documentado con reduced-motion.
---

# Estados y motion

## Estados por pantalla con datos

| Estado | Cómo se ve |
|---|---|
| Cargando | Skeletons con la forma del contenido (no un spinner en blanco) |
| Vacío | Mensaje y, si aplica, una acción |
| Error parcial | El resto de la pantalla se ve; la sección que falló muestra su mensaje y "Reintentar" |
| Error total | Mensaje claro y "Reintentar"; nunca pantalla en blanco |
| Sin sesión | Lleva al login |

Formularios: errores en línea por campo, error general arriba, botón principal con estado "cargando" y deshabilitado.

## Motion

- Duraciones y curvas de las variables `motion/*`.
- Entrada de listas o cards: aparición suave y escalonada (stagger corto).
- Documentar cada animación en `ux/handoff.md`: qué se anima, duración, curva, retraso.
- **Reduced-motion**: sin desplazamientos, solo cambio de opacidad (o nada). Documentado.

## Checklist

- [ ] Cada pantalla con datos tiene cargando, error parcial y error total
- [ ] Los formularios tienen errores en línea y botón en estado cargando
- [ ] Cada animación está documentada con su variante de reduced-motion
