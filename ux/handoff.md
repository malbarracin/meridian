#### Handoff — Meridian
Archivo de Penpot: https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=4b7520f4-628e-8193-8008-d4235058add0

> Archivo Penpot: **Meridian** (file-id `4b7520f4-628e-8193-8008-d4235058adcf`). Tablero base del Design System: **Design System / Meridian**. Páginas: `00 Design System`, `01 Web`, `02 Mobile`, `03 Flujos`.

##### Design System
Variables: [00 Design System](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=4b7520f4-628e-8193-8008-d4235058add0) · ver `ux/design-system.md`

- Tablero: Design System / Meridian · https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=4b7520f4-628e-8193-8008-d4235058add0
- Tokens: set `Tokens` — color, tipografía (Host Grotesk), space.1–8, radius.sm/md/lg, motion.fast/base
- Componentes (variantes + estados):
  - Button (primary/secondary · default, hover, focus, disabled, loading)
  - Input (default, focus, error, loading, disabled)
  - InlineError
  - MetricCard (default, hover, loading, error, reduced-motion)
  - ChartPanel (default, hover, loading, error, reduced-motion)
  - NewsCard (default, hover, loading, error, reduced-motion)
  - Skeleton (block, metric, chart, news)
  - ErrorBanner · ErrorScreen · MERIDIAN wordmark
- Microcopy de referencia en DS: "Ingresá un email válido", "La contraseña debe tener al menos 8 caracteres", "Cargando…", "Reintentar", "Abrir", "No pudimos cargar los mercados. Verificá tu conexión y volvé a intentar.", "[sección] no está disponible en este momento."
- Assets: wordmark texto `MERIDIAN` (componente MERIDIAN wordmark) · export SVG/PNG
- Motion: NewsCard entrada fade + stagger 100 ms · `motion.base` 250 ms · reduced-motion: sin translate, solo opacidad o sin animación
- Marca: MERIDIAN en layers/assets; no usar Soma / SOMA / Ops Deck

##### Register — web
- Tablero: Register / web · https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4
- Estados: default [Register / web](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4) · cargando [Register / web / loading](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4) · error inline [Register / web / error](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4) · error general [Register / web / error general](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4)
- Componentes: MERIDIAN wordmark · Input (default, disabled, error) · Button primary (default, loading) · ErrorBanner · password-toggle (texto "Mostrar")
- Microcopy: título "Crear cuenta" · CTA "Registrarse" · link "¿Ya tenés cuenta? Iniciá sesión" · toggle "Mostrar" · loading "Cargando…" · errores "Ingresá un email válido" · "La contraseña debe tener al menos 8 caracteres" · "Este email ya tiene una cuenta. Iniciá sesión" · "Algo salió mal. Intentá de nuevo."
- Datos: nombre, email, contraseña (formulario de registro; sin datos de API)
- Assets: wordmark MERIDIAN (componente) · export SVG/PNG
- Motion: CTA loading (spinner) · `motion.fast` 150 ms en hover/focus de Input y Button · reduced-motion: sin spinner animado (estado estático "Cargando…") o solo opacidad
- Marca: MERIDIAN en layers; sin Soma / SOMA / Ops Deck

##### Register — mobile
- Tablero: Register / mobile · https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f
- Estados: default [Register / mobile](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f) · cargando [Register / mobile / loading](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f) · error inline [Register / mobile / error](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f) · error general [Register / mobile / error general](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f)
- Componentes: MERIDIAN wordmark · Input (default, disabled, error) · Button primary (default, loading) · ErrorBanner · password-toggle (texto "Mostrar")
- Microcopy: título "Crear cuenta" · CTA "Registrarse" · link "¿Ya tenés cuenta? Iniciá sesión" · toggle "Mostrar" · loading "Cargando…" · errores "Ingresá un email válido" · "La contraseña debe tener al menos 8 caracteres" · "Algo salió mal. Intentá de nuevo."
- Datos: nombre, email, contraseña (formulario de registro; sin datos de API)
- Assets: wordmark MERIDIAN (componente) · export SVG/PNG
- Motion: CTA loading (spinner) · `motion.fast` 150 ms en hover/focus de Input y Button · reduced-motion: sin spinner animado (estado estático "Cargando…") o solo opacidad
- Marca: MERIDIAN en layers; sin Soma / SOMA / Ops Deck
- Layout: columna portrait 390×844 · form-card fill con padding 20 · CTA y campos ≥ 44 de alto

##### Login — web
- Tablero: Login / web · https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4
- Estados: default [Login / web](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4) · cargando [Login / web / loading](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4) · error inline [Login / web / error](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4) · error general [Login / web / error general](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4) · sesión expirada [Login / web / sesion expirada](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4)
- Componentes: MERIDIAN wordmark · Input (default, disabled, error) · Button primary (default, loading) · ErrorBanner · password-toggle (texto "Mostrar")
- Microcopy: título "Iniciar sesión" · CTA "Ingresar" · link "¿No tenés cuenta? Registrate" · toggle "Mostrar" · loading "Cargando…" · errores "Ingresá un email válido" · "Email o contraseña incorrectos" · "Algo salió mal. Intentá de nuevo." · "Tu sesión expiró. Volvé a ingresar."
- Datos: email, contraseña (formulario de login; sin datos de API)
- Assets: wordmark MERIDIAN (componente) · export SVG/PNG
- Motion: CTA loading (spinner) · `motion.fast` 150 ms en hover/focus de Input y Button · reduced-motion: sin spinner animado (estado estático "Cargando…") o solo opacidad
- Marca: MERIDIAN en layers; sin Soma / SOMA / Ops Deck

##### Login — mobile
- Tablero: Login / mobile · https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f
- Estados: default [Login / mobile](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f) · cargando [Login / mobile / loading](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f) · error inline [Login / mobile / error](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f) · error general [Login / mobile / error general](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f) · sesión expirada [Login / mobile / sesion expirada](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f)
- Componentes: MERIDIAN wordmark · Input (default, disabled, error) · Button primary (default, loading) · ErrorBanner (en mobile error general / sesión expirada: layout columna mensaje + Reintentar, detach local) · password-toggle (hit ≥44×44) · register-link (hit alto ≥44)
- Microcopy: título "Iniciar sesión" · CTA "Ingresar" · link "¿No tenés cuenta? Registrate" · toggle "Mostrar" · loading "Cargando…" · errores "Ingresá un email válido" · "Email o contraseña incorrectos" · "Algo salió mal. Intentá de nuevo." · "Tu sesión expiró. Volvé a ingresar."
- Datos: email, contraseña (formulario de login; sin datos de API)
- Assets: wordmark MERIDIAN (componente) · export SVG/PNG
- Motion: CTA loading (spinner) · `motion.fast` 150 ms en hover/focus de Input y Button · reduced-motion: sin spinner animado (estado estático "Cargando…") o solo opacidad
- Marca: MERIDIAN en layers; sin Soma / SOMA / Ops Deck
- Layout: columna portrait 390×844 · form-card fill con padding 20 · CTA, campos, password-toggle y register-link ≥ 44 de alto (áreas táctiles)

##### Dashboard — web
- Tablero: (pendiente TASK-6 / TASK-8)

##### Dashboard — mobile
- Tablero: (pendiente TASK-7 / TASK-8)
