#### Handoff — Meridian

Archivo de Penpot: https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=4b7520f4-628e-8193-8008-d4235058add0

> Archivo: **Meridian** · file-id `4b7520f4-628e-8193-8008-d4235058adcf` · team-id `65d5ee1e-9774-8045-8008-d0b3ae2e77c4`.
> Páginas del archivo: `00 Design System` (page-id `4b7520f4-628e-8193-8008-d4235058add0`) · `01 Web` (page-id `5f436e01-9f75-80bf-8008-d429041444f4`) · `02 Mobile` (page-id `5f436e01-9f75-80bf-8008-d429041c732f`) · `03 Flujos` (page-id `5f436e01-9f75-80bf-8008-d4290420d8ca`).
>
> **Cómo consumir este handoff:** el link abre la página del archivo; Penpot no codifica el frame en la URL, así que el tablero se busca por su **nombre exacto** (los que están en negrita o en el índice del final, con su `board-id`). Con el MCP de diseño: `penpotUtils.findShape(s => s.name === "<nombre del tablero>")` o `penpotUtils.findShapeById("<board-id>")`. Traer estructura y estilos con `penpot.generateMarkup([tablero], { type: "html" })` y `penpot.generateStyle([tablero], { type: "css" })`; tokens con `penpot.library.local.tokens` o con `ux/design-system.md`.
>
> Web y mobile son **la misma experiencia**: misma información y mismo Design System, distinto layout. Todos los valores salen de los tokens del Design System (ver `ux/design-system.md`); no hay colores, tamaños ni espacios sueltos.

##### Design System
Variables: [00 Design System](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=4b7520f4-628e-8193-8008-d4235058add0) · ver `ux/design-system.md`

- Tablero: **Design System / Meridian** · board-id `5f436e01-9f75-80bf-8008-d4298f15921d`
- Tokens: set activo `Tokens` — 39 variables: `color.*` (16) · `space.1`–`space.8` (8) · `radius.sm/md/lg` (3) · `font.family` (1) · `font.size-*` (5) · `font.weight-*` (4) · `motion.fast/base` (2)
- Componentes de la librería local (10): **Button**, **Input**, **InlineError**, **Skeleton**, **ErrorBanner**, **MetricCard**, **ChartPanel**, **NewsCard**, **ErrorScreen**, **MERIDIAN wordmark** (variantes y estados en `ux/design-system.md`; cada uno tiene su propio tablero con board-id en el índice) · **Tokens / color**: muestras visuales de 10 tokens de color
- Microcopy de referencia: "Crear cuenta" · "Iniciar sesión" · "Registrarse" · "Ingresar" · "Cargando…" · "Mostrar" · "Reintentar" · "Abrir" · "Salir" · "Ingresá un email válido" · "La contraseña debe tener al menos 8 caracteres" · "Este email ya tiene una cuenta. Iniciá sesión" · "Email o contraseña incorrectos" · "Algo salió mal. Intentá de nuevo." · "Tu sesión expiró. Volvé a ingresar." · "No pudimos cargar los mercados." · "Verificá tu conexión y volvé a intentar." · "[sección] no está disponible en este momento." · "No hay cotizaciones para mostrar." · "No hay datos de bolsa para mostrar." · "No hay datos de cripto para mostrar." · "No hay noticias por ahora."
- Assets: wordmark **MERIDIAN** (texto, componente `MERIDIAN wordmark`, 110×29) · exportar SVG (para web) y PNG @1x/@2x/@3x (para mobile) · los gráficos son parte del `ChartPanel` (no hay assets sueltos)
- Motion: `motion.base` 250 ms para la entrada de las NewsCard (fade + translateY corto) con stagger de 100 ms entre cards · `motion.fast` 150 ms en hover/focus de Button e Input · reduced-motion: sin translate ni stagger, solo opacidad o sin animación
- Marca: en capas, assets, tokens y textos solo se usa **MERIDIAN** · no se usan nombres alternativos del producto ni denominaciones de plataformas internas (lista de términos prohibidos en `specs/ux/marca.md`)
- A11y: contraste AA, foco visible (Button `primary` con anillo exterior), objetivos táctiles ≥ 44×44, variación de mercado con signo/flecha además del color

##### Register — web
- Tablero: **Register / web** · [01 Web](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4) · board-id `5f436e01-9f75-80bf-8008-d42c78bd2e65`
- Estados: default **Register / web** · cargando **Register / web / loading** · error inline **Register / web / error** · error email **Register / web / error email** · error general **Register / web / error general** · éxito/redirección **Register / web / éxito** (todos en [01 Web](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4); ids en el índice del final)
- Componentes: MERIDIAN wordmark · Input (default, disabled, error) · Button primary (default, loading) · ErrorBanner (error general: mensaje + botón "Reintentar") · InlineError (error inline bajo el campo) · password-toggle (texto "Mostrar") · login-link ("¿Ya tenés cuenta? Iniciá sesión")
- Microcopy: wordmark "MERIDIAN" · título "Crear cuenta" · labels "Nombre completo", "Email", "Contraseña" · toggle "Mostrar" · CTA "Registrarse" (único CTA primario) · loading "Cargando…" · link "¿Ya tenés cuenta? Iniciá sesión" · error inline "Ingresá un email válido" · "La contraseña debe tener al menos 8 caracteres" · error email "Este email ya tiene una cuenta. Iniciá sesión" (con link "Iniciá sesión") · error general "Algo salió mal. Intentá de nuevo." + "Reintentar"
- Datos: nombre completo, email y contraseña (formulario de registro; sin datos de API todavía — el login emite la sesión)
- Assets: wordmark MERIDIAN (componente) · export SVG/PNG
- Motion: CTA loading (spinner) · `motion.fast` 150 ms en hover/focus de Input y Button · éxito: redirección automática a Dashboard (sin pantalla intermedia) · reduced-motion: sin spinner animado (estado estático "Cargando…") o solo opacidad
- Layout: frame 1440×900 · `form-card` 464 px de ancho, padding 40 vertical / 32 horizontal, gap 20 entre bloques · `field` 400×44 con label arriba y error inline debajo (gap 6) · CTA 400×44 (padding 12/20)
- Éxito: el frame **Register / web / éxito** muestra el Dashboard ya cargado (destino tras el registro, sin UI de éxito separada)

##### Register — mobile
- Tablero: **Register / mobile** · [02 Mobile](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f) · board-id `5f436e01-9f75-80bf-8008-d42ec3d739e0`
- Estados: default **Register / mobile** · cargando **Register / mobile / loading** · error inline **Register / mobile / error** · error email **Register / mobile / error email** · error general **Register / mobile / error general** · éxito/redirección **Register / mobile / éxito** (todos en [02 Mobile](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f); ids en el índice del final)
- Componentes: MERIDIAN wordmark · Input (default, disabled, error) · Button primary (default, loading) · ErrorBanner (error general: mensaje + botón "Reintentar") · InlineError · password-toggle (texto "Mostrar", hit ≥ 44×44) · login-link (hit alto ≥ 44)
- Microcopy: wordmark "MERIDIAN" · título "Crear cuenta" · labels "Nombre completo", "Email", "Contraseña" · toggle "Mostrar" · CTA "Registrarse" (único CTA primario) · loading "Cargando…" · link "¿Ya tenés cuenta? Iniciá sesión" · error inline "Ingresá un email válido" · "La contraseña debe tener al menos 8 caracteres" · error email "Este email ya tiene una cuenta. Iniciá sesión" (con link "Iniciá sesión") · error general "Algo salió mal. Intentá de nuevo." + "Reintentar"
- Datos: nombre completo, email y contraseña (formulario de registro; sin datos de API todavía)
- Assets: wordmark MERIDIAN (componente) · export SVG/PNG
- Motion: CTA loading (spinner) · `motion.fast` 150 ms en hover/focus de Input y Button · éxito: redirección automática a Dashboard · reduced-motion: sin spinner animado (estado estático "Cargando…") o solo opacidad
- Layout: columna portrait 390×844 · `form-card` 358 px de ancho (20 de margen por lado), padding 32 vertical / 20 horizontal, gap 20 · campos, password-toggle, CTA y login-link con alto ≥ 44 (áreas táctiles)
- Éxito: el frame **Register / mobile / éxito** muestra el Dashboard ya cargado (destino tras el registro)

##### Login — web
- Tablero: **Login / web** · [01 Web](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4) · board-id `5f436e01-9f75-80bf-8008-d42ff2170568`
- Estados: default **Login / web** · cargando **Login / web / loading** · error inline **Login / web / error** · error general **Login / web / error general** · sesión expirada **Login / web / sesion expirada** · éxito/redirección **Login / web / éxito** (todos en [01 Web](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4); ids en el índice del final)
- Componentes: MERIDIAN wordmark · Input (default, disabled, error) · Button primary (default, loading) · ErrorBanner (error general y sesión expirada: mensaje + botón "Reintentar") · InlineError · password-toggle (texto "Mostrar")
- Microcopy: wordmark "MERIDIAN" · título "Iniciar sesión" · labels "Email", "Contraseña" · toggle "Mostrar" · CTA "Ingresar" (único CTA primario) · loading "Cargando…" · link "¿No tenés cuenta? Registrate" · error inline "Ingresá un email válido" · "Email o contraseña incorrectos" · error general "Algo salió mal. Intentá de nuevo." + "Reintentar" · sesión expirada "Tu sesión expiró. Volvé a ingresar." + "Reintentar"
- Datos: email y contraseña (formulario de login; sin datos de API todavía — el login emite la sesión)
- Assets: wordmark MERIDIAN (componente) · export SVG/PNG
- Motion: CTA loading (spinner) · `motion.fast` 150 ms en hover/focus de Input y Button · éxito: redirección automática a Dashboard (sin pantalla intermedia) · reduced-motion: sin spinner animado (estado estático "Cargando…") o solo opacidad
- Layout: frame 1440×900 · `form-card` 464 px de ancho, padding 40 vertical / 32 horizontal, gap 20 · `field` 400×44 con label arriba y error inline debajo (gap 6) · CTA 400×44 (padding 12/20)
- Éxito: el frame **Login / web / éxito** muestra el Dashboard ya cargado (destino tras el login)

##### Login — mobile
- Tablero: **Login / mobile** · [02 Mobile](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f) · board-id `5f436e01-9f75-80bf-8008-d4315c9430f8`
- Estados: default **Login / mobile** · cargando **Login / mobile / loading** · error inline **Login / mobile / error** · error general **Login / mobile / error general** · sesión expirada **Login / mobile / sesion expirada** · éxito/redirección **Login / mobile / éxito** (todos en [02 Mobile](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f); ids en el índice del final)
- Componentes: MERIDIAN wordmark · Input (default, disabled, error) · Button primary (default, loading) · ErrorBanner (error general y sesión expirada: layout en columna, mensaje arriba + botón "Reintentar" abajo, sin desborde) · InlineError · password-toggle (texto "Mostrar", hit ≥ 44×44) · register-link (hit alto ≥ 44)
- Microcopy: wordmark "MERIDIAN" · título "Iniciar sesión" · labels "Email", "Contraseña" · toggle "Mostrar" · CTA "Ingresar" (único CTA primario) · loading "Cargando…" · link "¿No tenés cuenta? Registrate" · error inline "Ingresá un email válido" · "Email o contraseña incorrectos" · error general "Algo salió mal. Intentá de nuevo." + "Reintentar" · sesión expirada "Tu sesión expiró. Volvé a ingresar." + "Reintentar"
- Datos: email y contraseña (formulario de login; sin datos de API todavía)
- Assets: wordmark MERIDIAN (componente) · export SVG/PNG
- Motion: CTA loading (spinner) · `motion.fast` 150 ms en hover/focus de Input y Button · éxito: redirección automática a Dashboard · reduced-motion: sin spinner animado (estado estático "Cargando…") o solo opacidad
- Layout: columna portrait 390×844 · `form-card` 358 px de ancho (20 de margen por lado), padding 32 vertical / 20 horizontal, gap 20 · campos, password-toggle, CTA y register-link con alto ≥ 44 (áreas táctiles)
- Éxito: el frame **Login / mobile / éxito** muestra el Dashboard ya cargado (destino tras el login)

##### Dashboard — web
- Tablero: **Dashboard / web** · [01 Web](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4) · board-id `5f436e01-9f75-80bf-8008-d4342da2da9e`
- Estados: default **Dashboard / web** · cargando **Dashboard / web / cargando** · vacío **Dashboard / web / vacío** · error parcial **Dashboard / web / error parcial** · error total **Dashboard / web / error total** · sin sesión **Dashboard / web / sin sesión** · reduced-motion **Dashboard / web / reduced-motion** (todos en [01 Web](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041444f4); ids en el índice del final)
- Componentes: MERIDIAN wordmark · Button secondary ("Salir") · MetricCard ×4 (Dólar oficial, Dólar blue, BTC, ETH) · ChartPanel ×2 (Bolsa, Cripto) · NewsCard ×3 · Skeleton (metric ×4, chart ×2, news ×3) para el estado cargando · EmptyPanel (título + mensaje + Button secondary "Reintentar") para el estado vacío · ChartPanel en estado error (error parcial) · ErrorScreen (error total) · Login + ErrorBanner de sesión expirada (sin sesión)
- Microcopy: header "MERIDIAN" · usuario "Ana García" · botón "Salir" · hero fecha "Jueves 22 de octubre de 2026" · título "Pulso de mercados" · métricas "Dólar oficial" "$ 1.015,00" "▲ +0,5%" "· cierre ant." "Actualizado 14:22" · "Dólar blue" "$ 1.245,00" "▲ +0,8%" "· cierre ant." "Actualizado 14:22" · "BTC" "USD 67.420,15" "▲ +2,4%" "· 24h" "Actualizado 14:22" · "ETH" "USD 3.218,40" "▼ −1,1%" "· 24h" "Actualizado 14:22" · panels "S&P Merval" "1.842.350" "▲ +1,2%" "7 días" "Resumen: tendencia alcista en la última semana." y "Cripto · BTC / ETH" "BTC 67,4k" "▲ +2,4%" "7 días" "Resumen: BTC lidera la semana; ETH retrocede levemente en 24h." · sección "Noticias IA y tecnología" · NewsCards "IA y chips: el nuevo ciclo de inversión en infraestructura" · "TechCrunch" · "22 oct 2026" · "Señales de mercado alrededor de centros de datos y semiconductores impulsan el interés en infraestructura de IA." · "Abrir" / "Modelos abiertos aceleran la competencia en IA generativa" · "The Verge" · "21 oct 2026" · "Nuevos lanzamientos open-source comprimen costos y reordenan prioridades de inversión en software." · "Abrir" / "Semiconductores: demanda de GPU sostiene el rally tech" · "Bloomberg" · "20 oct 2026" · "La cadena de suministro de aceleradores sigue tensionada; analistas miran guías del próximo trimestre." · "Abrir" · vacío "Cotizaciones" + "No hay cotizaciones para mostrar." / "Bolsa" + "No hay datos de bolsa para mostrar." / "Cripto" + "No hay datos de cripto para mostrar." / "Noticias IA y tecnología" + "No hay noticias por ahora." + "Reintentar" · error parcial "Bolsa" + "Bolsa no está disponible en este momento." + "Reintentar" · error total "No pudimos cargar los mercados." + "Verificá tu conexión y volvé a intentar." + "Reintentar" · sin sesión "Tu sesión expiró. Volvé a ingresar." + "Reintentar"
- Datos: Dólar oficial y blue (precio, variación, timestamp de actualización) ← cotización de dólar de la API pública argentina · BTC y ETH (precio en USD, variación 24h) ← API pública cripto · S&P Merval (valor, variación, serie de 7 días para el gráfico, resumen) ← API pública de bolsa · BTC/ETH (serie de 7 días, resumen) ← API pública cripto · Noticias IA/Tech (título, fuente, fecha, resumen, url del artículo para "Abrir") ← feed de noticias. Todas llegan procesadas por el backend adapter; el frontend solo consume estos campos (contrato en `specs/ux/pantallas.md` §5)
- Assets: wordmark MERIDIAN (componente) · export SVG/PNG · los gráficos (barras + `chart-line`) son parte del ChartPanel, no hay assets sueltos
- Motion: entrada de las NewsCard con fade + translateY corto, `motion.base` 250 ms, stagger de 100 ms entre cards · `motion.fast` 150 ms en hover/focus · reduced-motion: sin translate ni stagger, solo opacidad o sin animación (frame **Dashboard / web / reduced-motion**; el grid se anota "News grid · reduced-motion: sin translate / stagger (solo opacidad o sin animación)")
- Layout: frame 1440×922, gap 24 entre zonas · Header 1440×76 (padding 16/48) con wordmark, usuario y "Salir" · Hero (fecha + título, gap 8) · Fila superior: 4 MetricCard de 324×137 con gap 16 · Fila media: 2 ChartPanel de 664×259 con gap 16 · Fila inferior: grid de 3 NewsCard de 437×211 con gap 16 · error parcial: solo falla la sección de Bolsa, el resto del dashboard sigue visible · sin sesión: redirección a Login (mismo contenido que el estado de sesión expirada de Login)
- Estados: cargando = skeletons en todas las secciones · vacío = un EmptyPanel por sección · error parcial = una sección con mensaje + "Reintentar" · error total = ErrorScreen a pantalla completa · sin sesión = redirección a Login · reduced-motion = misma composición sin animaciones de entrada

##### Dashboard — mobile
- Tablero: **Dashboard / mobile** · [02 Mobile](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f) · board-id `5f436e01-9f75-80bf-8008-d436285a90e4`
- Estados: default **Dashboard / mobile** · cargando **Dashboard / mobile / cargando** · vacío **Dashboard / mobile / vacío** · error parcial **Dashboard / mobile / error parcial** · error total **Dashboard / mobile / error total** · sin sesión **Dashboard / mobile / sin sesión** · reduced-motion **Dashboard / mobile / reduced-motion** (todos en [02 Mobile](https://design.penpot.app/#/workspace?team-id=65d5ee1e-9774-8045-8008-d0b3ae2e77c4&file-id=4b7520f4-628e-8193-8008-d4235058adcf&page-id=5f436e01-9f75-80bf-8008-d429041c732f); ids en el índice del final)
- Componentes: MERIDIAN wordmark · Button secondary ("Salir") · MetricCard ×4 (Dólar oficial, Dólar blue, BTC, ETH) · ChartPanel ×2 (Bolsa, Cripto) · NewsCard ×3 · Skeleton (metric, chart, news) para el estado cargando · EmptyPanel (título + mensaje + Button secondary "Reintentar") para el estado vacío · ChartPanel en estado error (error parcial, solo Bolsa) · ErrorScreen (error total) · Login + ErrorBanner de sesión expirada (sin sesión)
- Microcopy: header "MERIDIAN" · usuario "Ana García" · botón "Salir" · hero fecha "Jueves 22 de octubre de 2026" · título "Pulso de mercados" · métricas "Dólar oficial" "$ 1.015,00" "▲ +0,5%" "· cierre ant." "Actualizado 14:22" · "Dólar blue" "$ 1.245,00" "▲ +0,8%" "· cierre ant." "Actualizado 14:22" · "BTC" "USD 67.420,15" "▲ +2,4%" "· 24h" "Actualizado 14:22" · "ETH" "USD 3.218,40" "▼ −1,1%" "· 24h" "Actualizado 14:22" · panels "S&P Merval" "1.842.350" "▲ +1,2%" "7 días" "Resumen: tendencia alcista en la última semana." y "Cripto · BTC / ETH" "BTC 67,4k" "▲ +2,4%" "7 días" "Resumen: BTC lidera la semana; ETH retrocede levemente en 24h." · sección "Noticias IA y tecnología" · NewsCards "IA y chips: el nuevo ciclo de inversión en infraestructura" · "TechCrunch" · "22 oct 2026" · "Señales de mercado alrededor de centros de datos y semiconductores impulsan el interés en infraestructura de IA." · "Abrir" / "Modelos abiertos aceleran la competencia en IA generativa" · "The Verge" · "21 oct 2026" · "Nuevos lanzamientos open-source comprimen costos y reordenan prioridades de inversión en software." · "Abrir" / "Semiconductores: demanda de GPU sostiene el rally tech" · "Bloomberg" · "20 oct 2026" · "La cadena de suministro de aceleradores sigue tensionada; analistas miran guías del próximo trimestre." · "Abrir" · vacío "Dólar" + "No hay cotizaciones para mostrar." / "Bolsa" + "No hay datos de bolsa para mostrar." / "Cripto" + "No hay datos de cripto para mostrar." / "Noticias" + "No hay noticias por ahora." + "Reintentar" · error parcial "Bolsa" + "Bolsa no está disponible en este momento." + "Reintentar" · error total "No pudimos cargar los mercados." + "Verificá tu conexión y volvé a intentar." + "Reintentar" · sin sesión "Tu sesión expiró. Volvé a ingresar." + "Reintentar"
- Datos: mismos campos que web (Dólar oficial y blue, BTC y ETH, S&P Merval, serie cripto 7d, noticias IA/Tech) ← APIs públicas vía backend adapter; el frontend consume precio, variación, timestamp, serie de 7 días, resumen y datos de la noticia
- Assets: wordmark MERIDIAN (componente) · export SVG/PNG · los gráficos son parte del ChartPanel
- Motion: entrada de las NewsCard con fade + translateY corto, `motion.base` 250 ms, stagger de 100 ms entre cards · `motion.fast` 150 ms en hover/focus · reduced-motion: sin translate ni stagger, solo opacidad o sin animación (frame **Dashboard / mobile / reduced-motion**; la columna se anota "Columna NewsCards · motion.base 250ms stagger 100ms")
- Layout: columna portrait 390 de ancho con scroll vertical (frame 390×2126), gap 20 entre zonas · Header 390×68 (padding 12/16) con wordmark, usuario y "Salir" · Hero (fecha + título) · Sección Dólar: 2 MetricCard de 358×137 apiladas · Sección Cripto: 2 MetricCard de 358×137 apiladas · Sección Bolsa: ChartPanel Bolsa (358×299) y ChartPanel Cripto (358×293) apilados; el gráfico usa 326 px de ancho (content del panel) y la línea sigue los máximos de las barras · Sección Noticias: 3 NewsCard de 358×211 apiladas · botones "Salir" y "Abrir" con alto ≥ 44 · error parcial: métricas, cripto y noticias visibles; solo Bolsa muestra el mensaje inline · sin sesión: redirección a Login
- Estados: cargando = skeletons en todas las secciones · vacío = un EmptyPanel por sección · error parcial = una sección con mensaje + "Reintentar" · error total = ErrorScreen · sin sesión = redirección a Login · reduced-motion = misma composición sin animaciones de entrada

##### Flujos (para web y mobile)

- F0 Registro → Dashboard: Register (default → loading → error o éxito) → redirección automática al Dashboard; no hay pantalla de éxito.
- F1 Login → Dashboard: Login (default → loading → error o éxito) → redirección automática al Dashboard.
- F2 Dashboard autenticado: cada sección carga con skeleton; si una sección falla, muestra error parcial y las demás siguen operativas; si todas fallan, ErrorScreen.
- F3 Cierre de sesión: botón "Salir" del header → se borra la sesión → redirección a Login.
- F4 Acceso sin sesión: ruta protegida sin sesión → redirección a Login (estado "sesión expirada").
- Navegación: Register → "¿Ya tenés cuenta? Iniciá sesión" → Login · Login → "¿No tenés cuenta? Registrate" → Register.
- La página `03 Flujos` del archivo está vacía: los flujos F0–F4 están descriptos acá en texto y son la fuente para el front.

##### Índice de tableros (nombre exacto → board-id)

| Tablero | Página | board-id |
|---|---|---|
| Design System / Meridian | 00 Design System | `5f436e01-9f75-80bf-8008-d4298f15921d` |
| Button | 00 Design System | `5f436e01-9f75-80bf-8008-d429370a6700` |
| Input | 00 Design System | `5f436e01-9f75-80bf-8008-d4294b9deb6f` |
| InlineError | 00 Design System | `5f436e01-9f75-80bf-8008-d42958fa9a09` |
| Skeleton | 00 Design System | `5f436e01-9f75-80bf-8008-d42959925b18` |
| ErrorBanner | 00 Design System | `5f436e01-9f75-80bf-8008-d42959ab06a3` |
| MetricCard | 00 Design System | `5f436e01-9f75-80bf-8008-d42963b03579` |
| ChartPanel | 00 Design System | `5f436e01-9f75-80bf-8008-d42972b7dba8` |
| NewsCard | 00 Design System | `5f436e01-9f75-80bf-8008-d4297ff2eae7` |
| ErrorScreen | 00 Design System | `5f436e01-9f75-80bf-8008-d4298da8911d` |
| MERIDIAN wordmark | 00 Design System | `5f436e01-9f75-80bf-8008-d4298de98f59` |
| Tokens / color | 00 Design System | `5f436e01-9f75-80bf-8008-d4298dfd28fe` |
| retry | 00 Design System | `5f436e01-9f75-80bf-8008-d42b0bdf26d9` |
| Register / web | 01 Web | `5f436e01-9f75-80bf-8008-d42c78bd2e65` |
| Register / web / loading | 01 Web | `5f436e01-9f75-80bf-8008-d42c87978520` |
| Register / web / error | 01 Web | `5f436e01-9f75-80bf-8008-d42c9f280c2d` |
| Register / web / error email | 01 Web | `5f436e01-9f75-80bf-8008-d43af9413461` |
| Register / web / error general | 01 Web | `5f436e01-9f75-80bf-8008-d42c8daaeaed` |
| Register / web / éxito | 01 Web | `5f436e01-9f75-80bf-8008-d43af80ec7f5` |
| Login / web | 01 Web | `5f436e01-9f75-80bf-8008-d42ff2170568` |
| Login / web / loading | 01 Web | `5f436e01-9f75-80bf-8008-d42ff29cfec5` |
| Login / web / error | 01 Web | `5f436e01-9f75-80bf-8008-d42ff341305e` |
| Login / web / error general | 01 Web | `5f436e01-9f75-80bf-8008-d42ff3e81ed5` |
| Login / web / sesion expirada | 01 Web | `5f436e01-9f75-80bf-8008-d42ff4a5f994` |
| Login / web / éxito | 01 Web | `5f436e01-9f75-80bf-8008-d43af8b7925e` |
| Dashboard / web | 01 Web | `5f436e01-9f75-80bf-8008-d4342da2da9e` |
| Dashboard / web / cargando | 01 Web | `5f436e01-9f75-80bf-8008-d4344558adca` |
| Dashboard / web / vacío | 01 Web | `5f436e01-9f75-80bf-8008-d43886fefdde` |
| Dashboard / web / error parcial | 01 Web | `5f436e01-9f75-80bf-8008-d43446f295c7` |
| Dashboard / web / error total | 01 Web | `5f436e01-9f75-80bf-8008-d434493469bb` |
| Dashboard / web / sin sesión | 01 Web | `5f436e01-9f75-80bf-8008-d4388da58284` |
| Dashboard / web / reduced-motion | 01 Web | `5f436e01-9f75-80bf-8008-d43879d7ce4f` |
| field / Contraseña | 01 Web | `5f436e01-9f75-80bf-8008-d42c620ba7ea` |
| label-row | 01 Web | `5f436e01-9f75-80bf-8008-d42c621454dc` |
| Header | 01 Web | `5f436e01-9f75-80bf-8008-d43410da9631` |
| user-actions | 01 Web | `5f436e01-9f75-80bf-8008-d4341109f353` |
| Register / mobile | 02 Mobile | `5f436e01-9f75-80bf-8008-d42ec3d739e0` |
| Register / mobile / loading | 02 Mobile | `5f436e01-9f75-80bf-8008-d42ec64dd4c4` |
| Register / mobile / error | 02 Mobile | `5f436e01-9f75-80bf-8008-d42ec8a443c4` |
| Register / mobile / error email | 02 Mobile | `5f436e01-9f75-80bf-8008-d43b1dac7942` |
| Register / mobile / error general | 02 Mobile | `5f436e01-9f75-80bf-8008-d42ecb4c6eff` |
| Register / mobile / éxito | 02 Mobile | `5f436e01-9f75-80bf-8008-d43b1c7e2d83` |
| Login / mobile | 02 Mobile | `5f436e01-9f75-80bf-8008-d4315c9430f8` |
| Login / mobile / loading | 02 Mobile | `5f436e01-9f75-80bf-8008-d4315d173884` |
| Login / mobile / error | 02 Mobile | `5f436e01-9f75-80bf-8008-d4315d96c4bb` |
| Login / mobile / error general | 02 Mobile | `5f436e01-9f75-80bf-8008-d4315e24815f` |
| Login / mobile / sesion expirada | 02 Mobile | `5f436e01-9f75-80bf-8008-d4315ebe26b0` |
| Login / mobile / éxito | 02 Mobile | `5f436e01-9f75-80bf-8008-d43b1d14004d` |
| Dashboard / mobile | 02 Mobile | `5f436e01-9f75-80bf-8008-d436285a90e4` |
| Dashboard / mobile / cargando | 02 Mobile | `5f436e01-9f75-80bf-8008-d4389f5635d5` |
| Dashboard / mobile / vacío | 02 Mobile | `5f436e01-9f75-80bf-8008-d438ab4421c9` |
| Dashboard / mobile / error parcial | 02 Mobile | `5f436e01-9f75-80bf-8008-d438b4e8a5ef` |
| Dashboard / mobile / error total | 02 Mobile | `5f436e01-9f75-80bf-8008-d438bd9b65a0` |
| Dashboard / mobile / sin sesión | 02 Mobile | `5f436e01-9f75-80bf-8008-d438bdf3a67f` |
| Dashboard / mobile / reduced-motion | 02 Mobile | `5f436e01-9f75-80bf-8008-d43896756eae` |

> Tableros auxiliares (no son pantallas): **Tokens / color** muestra 10 muestras de tokens de color · **Header** (1440×61) es una plantilla suelta con el wordmark y **label-row** (100×16) un fragmento con el label "Contraseña" · **field / Contraseña**, **user-actions** y **retry** están vacíos y no se usan. Los frames reales de pantalla son los 49 listados arriba.
