---
name: ux-specs
description: Convierte la especificación de producto en requisitos de UX (flujos, pantallas, contenido, estados y restricciones de marca) en specs/ux/.
---

# Requisitos de UX desde la especificación

## Qué escribir (`spec_write`, área `ux`, una spec por tema)

1. **Flujos**: cada camino del usuario, paso a paso (ej. registro → sesión → pantalla principal; salir → login).
2. **Pantallas por canal** (web, mobile): para cada una, qué contenido muestra **y en qué orden**, qué acciones hay
   y adónde llevan.
3. **Estados obligatorios** de cada pantalla: cargando, vacío, error parcial (una sección falla), error total, sin
   sesión, éxito.
4. **Marca y contenido**: nombre del producto tal cual, textos sugeridos, palabras o contenidos **prohibidos**, qué NO
   debe ser una pantalla (ej. "no es un panel de KPIs operativos").
5. **Datos**: de dónde sale cada dato que se muestra (API, cálculo) y su forma (para diseñar con datos reales).
6. **Fuera de alcance**, textual.

## Checklist

- [ ] Cada flujo de la especificación tiene sus pantallas listadas por canal
- [ ] Cada pantalla tiene su contenido en orden y sus estados
- [ ] Las restricciones de marca y contenido están copiadas textuales
- [ ] Lo que no se sabe y cambia el diseño quedó como pregunta, no inventado
