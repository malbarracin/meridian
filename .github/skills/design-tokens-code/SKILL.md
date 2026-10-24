---
name: design-tokens-code
description: Pasar los tokens del Design System (ux/design-system.md y tokens del archivo de diseño) a código — CSS variables/Tailwind en web y un tema tipado en mobile.
---

# Tokens en código

- **Fuente**: `ux/design-system.md` y los tokens del archivo de diseño. Mismo nombre que en Penpot (`color/primary` → `--color-primary`
  en web, `theme.color.primary` en mobile).
- **Web**: variables CSS en el CSS global y, si se usa Tailwind, mapeadas en su configuración.
- **Mobile**: `theme.ts` tipado (colores, tipografía, espacio, radios, motion) y un hook para leerlo.
- Modo claro y oscuro si el Design System los tiene.
- **Nunca** colores, tamaños o espacios sueltos en los componentes.

## Checklist

- [ ] Los nombres coinciden con Penpot
- [ ] Ningún componente tiene valores sueltos
