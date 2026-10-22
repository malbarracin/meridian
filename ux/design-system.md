# Design System — Meridian

Fuente: página Penpot `00 Design System` · tablero `Design System / Meridian`.
Archivo: [Meridian en Penpot](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=4b7520f4-628e-8193-8008-d4235058add0)

Nombre de producto en assets y layers: **MERIDIAN**. Prohibido: Soma, SOMA, SOMA / OPS, Ops Deck.

## Tokens

Colección activa: `Tokens` (modo claro). En Penpot los nombres usan punto (`color.bg`); en código se pueden mapear a CSS `--color-bg`.

### Color

| Variable | Claro | Uso |
|---|---|---|
| color.bg | `#F2F5F4` | Fondo de página |
| color.surface | `#FFFFFF` | Cards, paneles, inputs |
| color.text | `#0F1C1A` | Texto principal |
| color.text-muted | `#5A6B68` | Meta, timestamps, placeholders |
| color.primary | `#0D7377` | CTA, logo, foco, links |
| color.primary-hover | `#095456` | Hover de primario |
| color.primary-contrast | `#FFFFFF` | Texto sobre primario |
| color.positive | `#1B7A4E` | Variación alcista (con ▲) |
| color.negative | `#B3261E` | Variación bajista (con ▼) |
| color.border | `#D0D9D6` | Bordes de cards e inputs |
| color.focus | `#0D7377` | Anillo de foco (stroke 2) |
| color.error | `#B3261E` | Errores inline y banners |
| color.error-bg | `#FDECEA` | Fondo ErrorBanner |
| color.skeleton | `#E4EBE9` | Placeholders de carga |
| color.disabled | `#A8B5B2` | Texto deshabilitado |
| color.disabled-bg | `#E8EEEC` | Fondo deshabilitado |

### Tipografía

Familia: **Host Grotesk**.

| Variable | Valor |
|---|---|
| font.family | Host Grotesk |
| font.size-display | 32 |
| font.size-title | 20 |
| font.size-body | 16 |
| font.size-caption | 13 |
| font.size-metric | 28 |
| font.weight-regular | 400 |
| font.weight-medium | 500 |
| font.weight-semibold | 600 |
| font.weight-bold | 700 |

### Espacio

| Variable | Valor (px) |
|---|---|
| space.1 | 4 |
| space.2 | 8 |
| space.3 | 12 |
| space.4 | 16 |
| space.5 | 20 |
| space.6 | 24 |
| space.7 | 28 |
| space.8 | 32 |

### Radios

| Variable | Valor (px) |
|---|---|
| radius.sm | 4 |
| radius.md | 8 |
| radius.lg | 12 |

### Motion

| Variable | Valor | Uso |
|---|---|---|
| motion.fast | 150 ms | Hover, foco |
| motion.base | 250 ms | Entrada de cards |
| stagger NewsCard | 100 ms entre cards | Fade-in + translateY corto |
| reduced-motion | — | Sin translate; solo opacidad o sin animación |

## Componentes

| Componente | Variantes | Estados | Tokens |
|---|---|---|---|
| Button | primary, secondary | default, hover, focus, disabled, loading | color.primary*, color.surface, color.border, color.disabled*, radius.md |
| Input | — | default, focus, error, loading, disabled | color.surface, color.border, color.focus, color.error, color.disabled* |
| InlineError | — | default | color.error, font.size-caption |
| MetricCard | — | default, hover, loading, error, reduced-motion | color.surface, color.border, color.positive/negative, color.skeleton |
| ChartPanel | — | default, hover, loading, error, reduced-motion | idem + resumen en texto |
| NewsCard | — | default, hover, loading, error, reduced-motion | color.surface, color.primary (CTA Abrir), motion.base + stagger |
| Skeleton | block, metric, chart, news | loading | color.skeleton, radius.* |
| ErrorBanner | — | default | color.error-bg, color.error |
| ErrorScreen | — | default | color.surface, color.text, Button primary |
| MERIDIAN wordmark | — | default | color.primary, font.weight-bold |

### Estados documentados (por componente)

- **default**: estado en reposo.
- **hover**: borde o fondo acentuado con `color.primary` / `color.primary-hover`; transición `motion.fast`.
- **focus** (Button, Input): stroke 2 con `color.focus`. Button **primary/focus** usa stroke **outer** (el anillo se ve contra el fondo de página; inner sobre el mismo fill era invisible). Button **secondary/focus** e Input usan stroke inner contrastando con el fill.
- **loading**: spinner + label “Cargando…” / “Validando…” o skeleton con forma del contenido.
- **error**: mensaje textual + acción “Reintentar” (board ≥ 44×44) cuando aplica; Input muestra error inline bajo el campo.
- **reduced-motion**: mismo layout en reposo; en runtime sin desplazamiento (NewsCard: sin stagger/slide; solo fade o nada). MetricCard/ChartPanel no animan entrada.

### Accesibilidad

- Contraste AA: texto `#0F1C1A` / `#5A6B68` sobre `#FFFFFF`/`#F2F5F4`; CTA blanco sobre `#0D7377`.
- Objetivos táctiles ≥ 44×44: Button, Input, NewsCard CTA “Abrir”, ErrorBanner “Reintentar”, y “Reintentar” en MetricCard / ChartPanel / NewsCard (estado error).
- Variación de mercado: signo/flecha ▲▼ además del color.
- Gráficos (ChartPanel): título, valor, variación y resumen en texto.
