---
name: design-canvas
description: Cómo diseñar con el MCP de la herramienta de diseño del equipo — Penpot (execute_code con la Plugin API, high_level_overview, export_shape, import_image) o Figma (use_figma, get_screenshot) — sin romper archivos ajenos.
---

# Diseñar con el MCP de diseño

Usá el servidor de diseño que tenga tu rol: **`penpot`** (gratis) o **`figma`** (con licencia).

## Penpot

| Para | Herramienta |
|---|---|
| Ver el archivo abierto: páginas, tableros, componentes, tokens | `high_level_overview` |
| Consultar la API de plugins de Penpot antes de escribir código | `penpot_api_info` |
| Crear y editar: páginas, tableros (boards), formas, textos, componentes, tokens, flex layout | `execute_code` (JavaScript con el objeto `penpot`) |
| Ver cómo quedó un tablero (imagen) | `export_shape` |
| Subir imágenes (logo, íconos) | `import_image` |

- Penpot trabaja sobre **el archivo que la persona tiene abierto** en el navegador con el MCP conectado
  (Archivo → MCP Server → Conectar). Si el MCP responde que no hay archivo o plugin conectado, nota `BLOQUEADO:`
  pidiendo a la persona que abra el archivo del proyecto y lo conecte (no sigas sin él).
- Antes de cada `execute_code` grande, `penpot_api_info` del tema (ej. boards, flex layout, tokens, componentes):
  la API cambia entre versiones.
- Código chico y por partes (un tablero o un componente por llamada), y verificá con `high_level_overview`.

## Figma (si el equipo lo usa)

`whoami`, `create_new_file`, `use_figma` (crear y editar), `upload_assets`, `get_metadata` y `get_screenshot` (revisar).
Con un asiento gratuito solo se escribe en **Drafts**.

## Archivo del proyecto

1. Si `ux/handoff.md` ya tiene el link del archivo, usá **ese**. En Penpot, el archivo es el que está abierto: anotá
   su link (la URL del navegador que te pase la persona o la que figure en el handoff).
2. Páginas fijas: `00 Design System`, `01 Web`, `02 Mobile`, `03 Flujos`.

## Cómo construir

- **Tokens primero** (color, tipografía, espacio, radios), después **componentes** con variantes y estados, después
  **pantallas** que solo usan esos componentes y tokens.
- **Flex layout / auto layout** en todo: así web y mobile se adaptan y el front lo traduce a flex.
- Tableros: web 1440 de ancho, mobile 390; nombre `<Pantalla> / <canal> / <estado>` (ej. `Dashboard / web / cargando`).
- Textos reales de la especificación (marca, títulos, microcopy de errores), no "Lorem ipsum".
- Después de cada pantalla: `export_shape` (Penpot) o `get_screenshot` (Figma) para verla y corregir.

## Límites

- Nunca borrar ni renombrar archivos, páginas, componentes o tokens que no creó el equipo.
- Si la herramienta falla por permisos o plan, nota `BLOQUEADO:` con el error exacto.

## Checklist

- [ ] Trabajé en el archivo del proyecto (el link está en ux/handoff.md)
- [ ] Usé tokens y componentes del Design System, con flex/auto layout
- [ ] Cada tablero tiene el nombre Pantalla / canal / estado
- [ ] Revisé cada pantalla con su imagen exportada
