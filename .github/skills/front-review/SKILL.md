---
name: front-review
description: Revisión de código de front y fidelidad con Penpot — tokens, componentes, estados, textos, seguridad del cliente y accesibilidad.
---

# Revisión de front

1. **Fidelidad**: imagen del tablero del handoff (`export_shape` / `get_screenshot`) vs lo construido: layout, jerarquía, textos tal cual.
2. **Tokens y componentes**: nada de valores sueltos; componentes del Design System con su nombre.
3. **Estados y errores**: cargando, error parcial, error total, 401.
4. **Seguridad**: sin claves en el cliente, sin APIs de terceros desde el front, token donde dice la arquitectura.
5. **Accesibilidad**: labels, foco, contraste, reduced-motion.
6. **Pruebas**: cubren lo anterior.

Pedidos numerados: archivo, problema, qué hacer. Solo lo que falta o está mal.

## Checklist

- [ ] Comparé con el tablero de Penpot
- [ ] Revisé seguridad del cliente y estados
