---
name: design-to-code
description: Cómo traer un diseño con el MCP de diseño — Penpot (high_level_overview, execute_code con generateMarkup/generateStyle, export_shape) o Figma (get_design_context, get_variable_defs, get_screenshot) — y convertirlo en código del proyecto.
---

# Del diseño al código

## Pasos por pantalla

1. Tomá de `ux/handoff.md` el **tablero** de la pantalla (nombre `<Pantalla> / <canal>` y su link).
2. Traé su estructura y estilos:
   - **Penpot**: `high_level_overview` para ubicarlo; después `execute_code` buscando el tablero por nombre y
     `penpot.generateMarkup([tablero], { type: "html" })` y `penpot.generateStyle([tablero], { type: "css" })`
     (consultá `penpot_api_info` si algo cambió). Tokens: los del archivo (`penpot.library.local`) o `ux/design-system.md`.
   - **Figma**: `get_design_context` con el link (código de referencia React + Tailwind) y `get_variable_defs`.
   Si el Penpot no responde porque no hay archivo conectado, nota `BLOQUEADO:` pidiendo a la persona que lo abra y
   lo conecte (Archivo → MCP Server → Conectar).
3. Mapeá los estilos a los **tokens del tema** del proyecto (no copies valores sueltos).
4. Assets (íconos, imágenes) que marca el handoff: `export_shape` (Penpot) o `download_assets` (Figma), a la carpeta
   de assets de la app.
5. Escribí el componente **con los componentes y tokens del proyecto**: lo que genera la herramienta es una
   referencia, no se pega tal cual (en mobile se traduce a componentes de React Native).
6. Exportá la imagen del tablero (`export_shape` / `get_screenshot`) y compará con lo que construiste.

## Reglas

- Solo lectura en el archivo de diseño: nunca lo edites.
- Si el tablero no existe o el link no abre, nota `BLOQUEADO:` con el nombre (no inventes el diseño).

## Checklist

- [ ] Traje estructura y estilos del tablero del handoff
- [ ] Mapeé los estilos a los tokens del tema, sin valores sueltos
- [ ] Comparé con la imagen exportada
