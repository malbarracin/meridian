---
name: api-client-auth
description: Cliente de API del front — URL por entorno, token de sesión, 401 → login, errores parciales (degraded), nunca llamar a APIs de terceros desde el cliente.
---

# Cliente de API y sesión

1. Un solo módulo cliente (`lib/api`): URL base por variable de entorno, JSON, timeout.
2. Manda el token de sesión como dice el contrato (header `Authorization: Bearer` o cookie).
3. **401** en cualquier llamada → borrar sesión e ir al login.
4. **Errores parciales**: si la respuesta trae secciones caídas (ej. `degraded: ["crypto"]`), solo esa sección muestra
   su error con "Reintentar"; el resto se ve.
5. **Nunca** llamar desde el cliente a APIs de terceros ni poner sus claves: todo pasa por el backend del proyecto.
6. Tipos de las respuestas iguales al contrato de la API (si hay OpenAPI, generados o copiados de ahí).

## Checklist

- [ ] 401 lleva al login
- [ ] Una sección caída no rompe la pantalla
- [ ] Ninguna clave ni API de terceros en el cliente
