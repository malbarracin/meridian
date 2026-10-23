---
name: nextjs-app
description: Convenciones de una app Next.js (App Router, TypeScript estricto) — rutas protegidas, server/client components, fetch al backend propio, accesibilidad.
---

# App Next.js

- App Router, TypeScript `strict`. Server Components por defecto; `"use client"` solo donde hay estado o eventos.
- Rutas protegidas con middleware o en el layout del grupo: sin sesión → `/login`.
- Formularios accesibles (label, errores en línea con `aria-describedby`, botón deshabilitado mientras envía).
- La URL del backend por variable de entorno (`NEXT_PUBLIC_API_URL` o un proxy en route handlers); nunca claves en el
  cliente.
- `loading.tsx` y `error.tsx` por ruta con datos (o estados en el componente).
- Scripts: `dev`, `build`, `lint`, `typecheck` (`tsc --noEmit`), `test`, `verify`.

## Checklist

- [ ] Rutas protegidas redirigen al login
- [ ] verify corre typecheck, lint y test
