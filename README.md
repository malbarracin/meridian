# meridian

El servicio Java se encuentra en `services/auth-service`.

Desde la raíz, ejecutar `./mvnw -q -B verify` para compilar, probar y verificar
la cobertura. En Windows (PowerShell o cmd), usar `.\mvnw.cmd -q -B verify`.
Los lanzadores de la raíz delegan al Maven Wrapper del servicio y propagan
sus argumentos y código de salida. La suite incluye tests de integración con
Testcontainers: `verify` necesita un daemon Docker accesible (ver
`delivery/testing.md`).

Consultar `services/auth-service/README.md` para configurar y ejecutar el servicio.
