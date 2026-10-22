# Design System — Meridian

Fuente: página Penpot `00 Design System` · tablero **Design System / Meridian** (board-id `5f436e01-9f75-80bf-8008-d4298f15921d`).
Archivo: [Meridian en Penpot](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=4b7520f4-628e-8193-8008-d4235058add0)

Nombre de producto en assets, capas y textos: **MERIDIAN**. No se usan nombres alternativos del producto ni denominaciones de plataformas internas (lista de términos prohibidos en `specs/ux/marca.md`).

- Conjunto de tokens en Penpot: `Tokens` (activo, modo claro). No hay modo oscuro: el producto define un solo tema.
- En Penpot los nombres usan punto (`color.primary`); en web se mapean a variables CSS (`--color-primary`) y en mobile a un tema tipado (`theme.color.primary`). **Mismo nombre en los dos canales.**
- Ningún componente usa valores sueltos: todo sale de los tokens de este documento (mismos nombres y valores que en Penpot).

## Tokens

### Color

| Variable | Valor | Uso |
|---|---|---|
| `color.bg` | `#F2F5F4` | Fondo de página |
| `color.surface` | `#FFFFFF` | Cards, paneles, inputs |
| `color.text` | `#0F1C1A` | Texto principal |
| `color.text-muted` | `#5A6B68` | Meta, timestamps, placeholders |
| `color.primary` | `#0D7377` | CTA, wordmark, foco, links |
| `color.primary-hover` | `#095456` | Hover del primario |
| `color.primary-contrast` | `#FFFFFF` | Texto sobre el primario |
| `color.positive` | `#1B7A4E` | Variación alcista (con ▲) |
| `color.negative` | `#B3261E` | Variación bajista (con ▼) |
| `color.border` | `#D0D9D6` | Bordes de cards e inputs |
| `color.focus` | `#0D7377` | Anillo de foco (stroke 2) |
| `color.error` | `#B3261E` | Errores inline y banners |
| `color.error-bg` | `#FDECEA` | Fondo de ErrorBanner |
| `color.skeleton` | `#E4EBE9` | Placeholders de carga |
| `color.disabled` | `#A8B5B2` | Texto deshabilitado |
| `color.disabled-bg` | `#E8EEEC` | Fondo deshabilitado |

### Tipografía

Familia: **Host Grotesk** (pesos 400 / 500 / 600 / 700).

| Variable | Valor | Uso |
|---|---|---|
| `font.family` | `Host Grotesk` | Toda la interfaz |
| `font.size-display` | `32` | Título de pantalla (Register / Login) |
| `font.size-title` | `20` | Título de sección (Hero del Dashboard, título de card) |
| `font.size-body` | `16` | Texto de campo, body de card |
| `font.size-caption` | `13` | Meta, timestamps, mensajes de error |
| `font.size-metric` | `28` | Valor de la métrica (MetricCard, ChartPanel) |
| `font.weight-regular` | `400` | Texto común |
| `font.weight-medium` | `500` | Labels y meta |
| `font.weight-semibold` | `600` | Títulos de sección |
| `font.weight-bold` | `700` | Wordmark MERIDIAN |

### Espacio

| Variable | Valor (px) | Uso típico |
|---|---|---|
| `space.1` | 4 | Ajuste fino |
| `space.2` | 8 | Gap interno de un bloque |
| `space.3` | 12 | Gap de card, padding chico |
| `space.4` | 16 | Padding de card/panel, gap entre columnas |
| `space.5` | 20 | Gap entre bloques del formulario, margen mobile |
| `space.6` | 24 | Gap entre zonas del Dashboard web |
| `space.7` | 28 | — |
| `space.8` | 32 | Padding vertical del form-card web, padding bottom del board |

### Radios

| Variable | Valor (px) | Uso |
|---|---|---|
| `radius.sm` | 4 | Chips y barras de skeleton |
| `radius.md` | 8 | Button, Input, card, panel |
| `radius.lg` | 12 | Contenedor grande (form-card) |

### Motion

| Variable | Valor | Uso |
|---|---|---|
| `motion.fast` | 150 ms | Hover y foco de Button / Input |
| `motion.base` | 250 ms | Entrada de las NewsCard (fade + translateY corto) |
| stagger NewsCard | 100 ms entre cards | Escalonado de la entrada de la fila/columna de noticias |
| `reduced-motion` | — | Sin translate ni stagger: solo opacidad o sin animación |

## Componentes

Componentes de la librería local de Penpot (10) y sus variantes exactas:

| Componente | Variantes (ejes de Penpot) | Estados | Tokens que usa | Base (ancho × alto) |
|---|---|---|---|---|
| **Button** | `Variant`: primary, secondary | default, hover, focus, disabled, loading | `color.primary`, `color.primary-hover`, `color.primary-contrast`, `color.surface`, `color.border`, `color.text`, `color.disabled`, `color.disabled-bg`, `radius.md` | 160 × 44 (padding 12/20, gap 8) |
| **Input** | — | default, focus, error, loading, disabled | `color.surface`, `color.border`, `color.text`, `color.text-muted`, `color.focus`, `color.error`, `color.disabled`, `color.disabled-bg`, `radius.md` | 320 × 44 (error: 66 con mensaje inline) |
| **InlineError** | — | default | `color.error`, `font.size-caption` | 291 × 16 (capa `message`) |
| **Skeleton** | `Type`: block, metric, chart, news | loading | `color.skeleton`, `radius.*` | block 200×16 · metric 240×108 · chart 360×198 · news 320×162 |
| **ErrorBanner** | — | default | `color.error-bg`, `color.error`, `color.surface` | 480 × 68 (mensaje + botón "Reintentar", gap 16) |
| **MetricCard** | — | default, hover, loading, error, reduced-motion | `color.surface`, `color.border`, `color.text`, `color.text-muted`, `color.positive`, `color.negative`, `color.skeleton`, `radius.md` | 240 × 137 (default/hover/reduced-motion) · 128 (error) · 100 (loading) |
| **ChartPanel** | — | default, hover, loading, error, reduced-motion | `color.surface`, `color.border`, `color.text`, `color.text-muted`, `color.positive`, `color.negative`, `color.primary` (línea), `color.skeleton`, `radius.md` | 440 × 259 (default/hover/reduced-motion) · 222 (loading) · 137 (error) |
| **NewsCard** | — | default, hover, loading, error, reduced-motion | `color.surface`, `color.border`, `color.text`, `color.text-muted`, `color.primary` (CTA "Abrir"), `motion.base` + stagger | 320 × 211 (default/hover/reduced-motion) · 230 (error) · 162 (loading) |
| **ErrorScreen** | — | default | `color.surface`, `color.text`, `color.text-muted`, Button primary | 480 × 173 (gap 12) |
| **MERIDIAN wordmark** | — | default | `color.primary`, `font.weight-bold` | 110 × 29 |

> `EmptyPanel` no es un componente de librería: es el patrón de estado vacío/vacío por sección, compuesto por `empty-title` + `empty-message` + `Button` secondary "Reintentar" (gap 12). Se documenta acá porque el front lo necesita en los estados vacío y error parcial del Dashboard.

### Estructura interna (capas del componente)

- **Button**: `Button` (label) · en `loading` suma el spinner (16×16) y el label pasa a "Cargando…".
- **Input**: `field` (320×44, placeholder o valor) · en `loading` el campo muestra spinner + "Validando…" · el estado `error` agrega el `inline-error` debajo (alto 66 en total).
- **MetricCard**: `label` · `value` (28 px) · `change-row` (`change` con signo + % y `period`: "· cierre ant." o "· 24h") · `timestamp` ("Actualizado 14:22"). En `loading` muestra `sk-label` / `sk-value` / `sk-change` (alto 100); en `error` el valor pasa a "No disponible" y aparece `retry-row` con botón "Reintentar" (120 × 44, alto 128).
- **ChartPanel**: `header` (`title` + `meta` con `value`, `change`, `period` "7 días") · `chart` (11 `bar` + `chart-line`) · `summary` ("Resumen: …"). En `loading` muestra `sk-title` / `sk-meta` / `sk-chart` (alto 222); en `error` muestra `title` + `error-message` + botón "Reintentar" (120 × 44, alto 137).
- **NewsCard**: `title` · `meta` (`source` + `sep` "·" + `date`) · `summary` (2–3 líneas) · `cta` "Abrir" (56 × 44). En `loading` muestra `sk-0`…`sk-3` (alto 162); en `error` muestra `section` ("Noticias") + `error-message` + botón "Reintentar" (120 × 44, alto 230).
- **Skeleton**: según `Type` — `block` (línea genérica 200×16) · `metric` (`line1` / `line2` / `line3` 240×108) · `chart` (`title` + `area` 360×198) · `news` (`title` / `meta` / `summary` / `cta` 320×162). En las pantallas el skeleton se estira al ancho de la columna y conserva el alto del componente: web metric 324×108, chart 664×198, news 437×162 · mobile metric 358×108, chart 358×198, news 358×162.
- **ErrorBanner**: `message-wrap` (274×17) + `retry` ("Reintentar", 90 × 44).
- **ErrorScreen**: `title` (309×24) · `body` (245×17) · `retry` ("Reintentar", 140 × 44).

### Estados (definición)

- **default**: estado en reposo.
- **hover**: borde o fondo acentuado con `color.primary` / `color.primary-hover`; transición `motion.fast`.
- **focus** (Button, Input): stroke 2 con `color.focus`. El Button `primary/focus` usa stroke **outer** (el anillo se ve contra el fondo de página; un stroke inner del mismo color que el fill era invisible). Button `secondary/focus` e Input usan stroke inner contrastando con el fill.
- **disabled**: `color.disabled` sobre `color.disabled-bg`, sin acción.
- **loading**: spinner + label "Cargando…" en Button; skeleton con la forma del contenido en MetricCard, ChartPanel, NewsCard y Skeleton.
- **error**: mensaje textual + acción "Reintentar" (alto ≥ 44) cuando aplica; el Input muestra el error inline debajo del campo.
- **reduced-motion**: mismo layout en reposo; en runtime sin desplazamiento (NewsCard: sin stagger ni slide; solo fade o nada). MetricCard y ChartPanel no animan la entrada.

## Accesibilidad

- Contraste AA: texto `#0F1C1A` / `#5A6B68` sobre `#FFFFFF` / `#F2F5F4`; CTA `#FFFFFF` sobre `#0D7377`.
- Foco visible: anillo de 2 px con `color.focus` en todos los controles interactivos.
- Objetivos táctiles ≥ 44×44: Button, Input, CTA "Abrir" de NewsCard, "Reintentar" de ErrorBanner, ErrorScreen, MetricCard, ChartPanel y NewsCard.
- Variación de mercado: signo y flecha (▲ / ▼) además del color.
- Gráficos (ChartPanel): título, valor, variación, período y resumen siempre en texto, no solo en el dibujo.
- `prefers-reduced-motion`: sin translate ni stagger; solo opacidad o sin animación.

## Cómo lo consume cada canal

- **Web**: variables CSS en el CSS global (`--color-bg`, `--space-4`, `--radius-md`, `--motion-fast`…) y, si se usa Tailwind, mapeadas en su configuración. Un solo tema (claro).
- **Mobile**: `theme.ts` tipado con colores, tipografía, espacio, radios y motion, y un hook para leerlo. Mismos nombres que en Penpot.
- En los dos canales: los componentes se construyen desde los componentes del Design System, sin valores sueltos, y los estados y el motion salen de `ux/handoff.md`.
