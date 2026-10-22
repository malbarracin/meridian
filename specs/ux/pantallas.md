---
title: Meridian — Pantallas por canal
status: borrador
source: https://app.notion.com/p/3ab75bb08ca781719e02e27161a87aaf
updated: 2026-10-22T12:22:53.981Z
---

# Meridian — Pantallas por canal

Canales: **web** y **mobile**. Misma información y mismo Design System; distinto layout.

---

## 1. Pantalla: Register

### Web y Mobile (mismo contenido)

**Contenido en orden:**
1. Logo / texto `MERIDIAN`
2. Título: "Crear cuenta" (sugerido)
3. Campo: Nombre completo
4. Campo: Email
5. Campo: Contraseña (con toggle mostrar/ocultar)
6. CTA principal: "Registrarse" (único CTA primario)
7. Link secundario → Login: "¿Ya tenés cuenta? Iniciá sesión"

**Acciones:**
- Enviar formulario → F0 paso 4-7
- Click en link → navega a Login

**Estados:**

| Estado | Qué muestra |
| --- | --- |
| Default | Formulario vacío |
| Loading | CTA en estado loading (spinner o disable); campos bloqueados |
| Error inline campo | Mensaje de error debajo del campo con problema (ej. "El email ya está en uso", "La contraseña debe tener al menos 8 caracteres") |
| Error general | Banner o mensaje en la parte superior del formulario |
| Éxito | Redirección automática a Dashboard (no hay pantalla de "éxito" separada) |

**Microcopy de errores definido:**
- Email ya registrado: "Este email ya tiene una cuenta. [Iniciá sesión]"
- Email inválido: "Ingresá un email válido"
- Contraseña muy corta: "La contraseña debe tener al menos 8 caracteres"
- Error de servidor: "Algo salió mal. Intentá de nuevo."

---

## 2. Pantalla: Login

### Web y Mobile (mismo contenido)

**Contenido en orden:**
1. Logo / texto `MERIDIAN`
2. Título: "Iniciar sesión" (sugerido)
3. Campo: Email
4. Campo: Contraseña (con toggle mostrar/ocultar)
5. CTA principal: "Ingresar" (único CTA primario)
6. Link secundario → Register: "¿No tenés cuenta? Registrate"

**Acciones:**
- Enviar formulario → F1 paso 4-7
- Click en link → navega a Register

**Estados:**

| Estado | Qué muestra |
| --- | --- |
| Default | Formulario vacío |
| Loading | CTA en estado loading; campos bloqueados |
| Error credenciales | Mensaje inline: "Email o contraseña incorrectos" |
| Error de servidor | "Algo salió mal. Intentá de nuevo." |
| Sin sesión (redirect) | Pantalla Login con mensaje: "Tu sesión expiró. Volvé a ingresar." (banner) |
| Éxito | Redirección automática a Dashboard |

**Microcopy de errores definido:**
- Credenciales incorrectas: "Email o contraseña incorrectos"
- Email inválido (validación client-side): "Ingresá un email válido"
- Error de servidor: "Algo salió mal. Intentá de nuevo."
- Sesión expirada: "Tu sesión expiró. Volvé a ingresar."

---

## 3. Pantalla: Dashboard — Web

### Composición obligatoria (orden de zonas)

| Zona | Contenido |
| --- | --- |
| **Header** | `MERIDIAN` (logo/texto) • nombre de usuario + botón "Salir" |
| **Hero / fecha** | Fecha actual + título "Mercados" o "Pulso de mercados" |
| **Fila superior** | Card(s) Dólar + Cards BTC / ETH |
| **Fila media** | Panel Bolsa con gráfico histórico (línea/área) + Panel Cripto con gráfico (sparkline o chart 7d) |
| **Fila inferior** | Carrusel o grid de NewsCards IA-Tech (animación de entrada al aparecer) |

### Contenido detallado por sección

**Card Dólar:**
- Cotización(es): oficial / blue (u otras variantes disponibles)
- Variación respecto al cierre anterior (valor + %)
- Timestamp de última actualización
- Fuente del dato: API pública (backend adapter)

**Cards Cripto (BTC y ETH):**
- Nombre + símbolo (BTC, ETH)
- Precio en USD
- Variación 24h (valor + %)
- Gráfico sparkline o chart 7 días

**Panel Bolsa:**
- Nombre del índice o ticker principal
- Precio / valor actual
- Variación (valor + %)
- Gráfico histórico: línea o área (período configurable o fijo; mínimo 1 semana)
- Fuente del dato: API pública (backend adapter)

**Panel Cripto (gráfico):**
- Gráfico comparativo o individual BTC/ETH (sparkline o chart 7d)
- Puede ser parte del panel cripto o sección separada

**NewsCards IA/Tecnología:**
- Título del artículo
- Fuente (nombre del medio)
- Fecha de publicación
- Resumen corto (2-3 líneas)
- CTA: "Abrir" (abre el artículo externo)
- Animación de entrada: suave / stagger al aparecer en viewport
- Layout: carrusel horizontal o grid

### Estados del Dashboard Web

| Estado | Qué muestra |
| --- | --- |
| Loading (global) | Skeletons en todas las secciones |
| Loading (por sección) | Skeleton en la sección que carga; resto visible |
| Datos cargados | Vista completa con datos reales |
| Error parcial (1 sección falla) | Sección fallida muestra mensaje de error + CTA "Reintentar"; resto del dashboard operativo |
| Error total | Pantalla de error completa + CTA "Reintentar" |
| Sin sesión | Redirección a Login (no se muestra el dashboard) |
| Sesión expirada | Redirección a Login con mensaje "Tu sesión expiró." |
| Reduced-motion | Animaciones de NewsCards reducidas o eliminadas |

**Microcopy de error parcial:** "[Nombre de sección] no está disponible en este momento. [Reintentar]"
**Microcopy de error total:** "No pudimos cargar los mercados. Verificá tu conexión y [volvé a intentar]."

---

## 4. Pantalla: Dashboard — Mobile

### Composición obligatoria (columna, scroll vertical)

Orden top → bottom:
1. **Header**: `MERIDIAN` (logo/texto) • ícono de usuario o "Salir"
2. **Fecha + título**: Fecha + "Mercados"
3. **Sección Dólar**: Card(s) apiladas
4. **Sección Cripto**: Cards BTC / ETH apiladas
5. **Sección Bolsa**: Panel con gráfico histórico (legible en portrait)
6. **Sección Noticias**: Cards apiladas con animación de entrada corta

### Reglas de layout mobile

- Columna única; sin tabs obligatorios (puede haber scroll por sección)
- Charts legibles en portrait: altura mínima suficiente para leer tendencias
- NewsCards apiladas verticalmente con animación de entrada corta (stagger o fade)
- Respetar `prefers-reduced-motion`

### Estados del Dashboard Mobile

Idénticos a web (ver tabla anterior); las mismas secciones, mismo comportamiento de error parcial / total / skeleton / reduced-motion.

---

## 5. Datos: origen y forma

| Dato | Origen | Forma esperada |
| --- | --- | --- |
| Dólar (oficial/blue) | API pública argentina (ej. dolarapi.com u otro) — backend adapter | Número con 2 decimales + signo variación |
| Bolsa (índice/ticker) | API pública (ej. Alpha Vantage, Yahoo Finance u otro) — backend adapter | Precio + variación + array de puntos para gráfico |
| BTC / ETH | API pública cripto (ej. CoinGecko u otro) — backend adapter | Precio USD + variación 24h + array sparkline |
| Noticias IA/Tech | API pública (ej. NewsAPI u otro) — backend adapter | título, fuente, fecha, resumen, url |

> Las API keys se guardan **solo en el backend**; el frontend recibe los datos ya procesados.

---

## 6. Design System — Componentes mínimos necesarios

| Componente | Uso |
| --- | --- |
| `MetricCard` | Dólar, BTC, ETH: dato principal + variación + timestamp |
| `ChartPanel` | Bolsa y cripto: gráfico histórico (línea/área) + metadata |
| `NewsCard` | Noticias: título + fuente + fecha + resumen + CTA |
| Skeleton | Estado loading de cada componente |
| ErrorBanner | Error parcial dentro de una sección |
| ErrorScreen | Error total del dashboard |

---

## 7. Supuestos (decisiones de UX tomadas sin bloqueo)

- El orden de secciones en mobile es Dólar → Cripto → Bolsa → Noticias (priorizando las cotizaciones más rápidas de leer primero).
- El gráfico de bolsa en mobile se muestra en portrait con altura fija; no se colapsa.
- Las NewsCards usan animación fade-in + stagger (100ms entre cards); con `prefers-reduced-motion` se muestran sin animación.
- No hay pantalla de "éxito" tras registro: el flujo va directo al Dashboard.
- El dashboard no tiene filtros por defecto (fuera de alcance por ahora).
