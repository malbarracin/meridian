---
name: front-testing
description: Pruebas de front — componentes y pantallas (Testing Library), estados, errores de la API simulados, 401, accesibilidad básica.
---

# Pruebas de front

- Web: Vitest o Jest + Testing Library. Mobile: jest-expo + Testing Library de React Native.
- API simulada (MSW o mocks del módulo cliente): **nunca** llamadas reales en las pruebas.
- Por pantalla: se ve con datos; cargando; error parcial (una sección); error total; 401 → login.
- Formularios: validación en línea, envío, error del servidor.
- Accesibilidad básica: roles y labels encontrables (`getByRole`, `getByLabelText`).
- `npm run verify` en verde antes de terminar.

## Checklist

- [ ] Cada pantalla con datos tiene pruebas de sus estados
- [ ] 401 probado
- [ ] verify en verde
