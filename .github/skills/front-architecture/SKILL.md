---
name: front-architecture
description: Arquitectura de una app de front (web Next.js o mobile Expo) — carpetas, navegación, estado, cliente de API, sesión, tokens y pruebas.
---

# Arquitectura de front

## Qué dejar en `specs/front/` (una spec por canal)

1. **Stack y carpeta** (`apps/web` o `apps/mobile`), versión de Node, gestor de paquetes (npm) y scripts:
   `dev`, `build`, `test`, `lint`, `typecheck` y **`verify`** (= typecheck + lint + test; es lo que corre opensoma).
2. **Navegación**: rutas o pantallas, cuáles requieren sesión y adónde va cada una sin sesión.
3. **Cliente de API**: URL base por variable de entorno, cómo manda el token, qué hace con 401 (cerrar sesión e ir
   al login) y con errores parciales (`degraded[]` o equivalentes).
4. **Sesión**: dónde se guarda el token (web: cookie httpOnly vía la propia app o memoria; mobile: SecureStore) y
   cuándo se borra.
5. **Tokens**: de dónde salen (`ux/design-system.md` + tokens del archivo de diseño) y en qué archivo viven.
6. **Componentes**: los del Design System con el mismo nombre.
7. **Pruebas**: herramienta y qué se prueba como mínimo.

## Checklist

- [ ] Existe el script verify y dice qué corre
- [ ] Cada ruta dice si requiere sesión
- [ ] El cliente de API maneja 401 y errores parciales
- [ ] Los tokens salen del Design System
