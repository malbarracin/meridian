---
title: Meridian — Marca y restricciones de contenido
status: borrador
source: https://app.notion.com/p/3ab75bb08ca781719e02e27161a87aaf
updated: 2026-10-22T12:22:26.471Z
---

# Meridian — Marca y restricciones de contenido

> Estas restricciones son ley para el diseño. Aplican a textos, microcopy, nombres de capas, variables, tokens, componentes y archivos.

---

## Nombre del producto

| Campo | Valor |
| --- | --- |
| Nombre de producto | **Meridian** |
| Header / logo text | `MERIDIAN` (mayúsculas) |
| Tagline sugerido | Mercados y señales, en un solo lugar |
| Título de pantalla dashboard sugerido | **Mercados** o **Pulso de mercados** |

## Palabras y términos PROHIBIDOS en UI, microcopy, Figma, tokens y assets

- `Soma`
- `SOMA`
- `SOMA / OPS`
- `Ops Deck`
- `demo soma`
- "Pulso operativo" (título de dashboard genérico de ops internas)

## Lo que el Dashboard NO debe ser

- **No** es un panel de KPIs operativos (usuarios activos / sesiones / errores).
- **No** debe mostrar 3 KPI cards de "usuarios activos / sesiones / errores" como contenido principal.
- **No** mostrar métricas operativas internas como contenido del dashboard de Meridian.

## Contenido obligatorio del Dashboard

El dashboard **debe** mostrar:
1. Cotización del **dólar** (oficial / blue u otras variantes disponibles)
2. **Bolsa**: índices o acciones + gráficos históricos
3. **Cripto**: BTC y ETH + variación 24h + gráficos
4. **Noticias IA y tecnología**: cards con título, fuente, fecha, resumen corto y CTA "Abrir"

## Animaciones

- NewsCards tienen **animación al aparecer**: entrada suave / stagger.
- Respetar `prefers-reduced-motion`: si el usuario tiene esta preferencia activa, las animaciones se reducen o eliminan.
- No hacer animaciones cinematográficas largas (fuera de alcance).

## Fuera de alcance (textual desde spec)

- Trading / compra-venta
- Animaciones cinematográficas largas
- Tablet / foldables
- SSO / OAuth / MFA
- Verificación de email / reset password
- Roles complejos RBAC
- Persistencia productiva / multi-tenant
- Publicación en App Store / Play Store
