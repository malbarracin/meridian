---
title: Meridian — Flujos de usuario
status: borrador
source: https://app.notion.com/p/3ab75bb08ca781719e02e27161a87aaf
updated: 2026-10-22T12:22:26.569Z
---

# Meridian — Flujos de usuario

Canales: **web** y **mobile** (misma experiencia, distinto layout).

---

## F0 — Registro → Dashboard

1. Usuario abre la app por primera vez (o navega a `/register`)
2. Ve la pantalla **Register** con logo `MERIDIAN`
3. Completa nombre, email y contraseña
4. Si hay error de validación → mensaje inline en el campo correspondiente
5. Envía el formulario → CTA en estado **loading**
6. Registro exitoso → backend emite JWT → usuario queda con sesión activa
7. Redirección automática a **Dashboard (Mercados)**

## F1 — Login → Dashboard

1. Usuario navega a `/login` (o es redirigido desde cualquier ruta protegida)
2. Ve la pantalla **Login** con logo `MERIDIAN`
3. Completa email y contraseña
4. Si hay error de credenciales → mensaje inline debajo del campo password (o banner)
5. Envía el formulario → CTA en estado **loading**
6. Login exitoso → backend valida JWT → usuario queda con sesión activa
7. Redirección automática a **Dashboard (Mercados)**

## F2 — Dashboard autenticado

1. Cliente envía JWT en cada request al backend
2. Backend agrega datos de dólar + bolsa + cripto + noticias (vía adapters a APIs públicas)
3. UI muestra secciones en orden:
   - Dólar (cotizaciones)
   - Cripto (BTC + ETH)
   - Bolsa (índice/tickers + gráfico)
   - Noticias IA/Tecnología (feed de cards)
4. Cada sección carga con **skeleton** mientras espera datos
5. Si **una sección falla** → esa sección muestra mensaje de error parcial; las demás siguen operativas
6. Si **todo falla** → pantalla de error total (con opción de reintentar)

## F3 — Cierre de sesión

1. Usuario hace clic en "Salir" en el header
2. Se elimina el JWT localmente
3. Redirección a **Login**

## F4 — Acceso sin sesión (protección de ruta)

1. Usuario intenta acceder al Dashboard sin JWT válido
2. Es redirigido a **Login**
3. Tras login exitoso, es redirigido al Dashboard

---

## Navegación entre Register y Login

- Pantalla Register tiene link → Login ("¿Ya tenés cuenta? Iniciá sesión")
- Pantalla Login tiene link → Register ("¿No tenés cuenta? Registrate")
