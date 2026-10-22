# auth-service

Base del servicio de autenticación con Java 21, Spring Boot y MongoDB.

Desde esta carpeta, compilar y probar con `./mvnw clean verify`.
En PowerShell usar `.\mvnw.cmd clean verify`.

Para ejecutar, definir `MONGO_URI` (URI de MongoDB) y `MONGO_DATABASE` (nombre de la base),
y correr `./mvnw spring-boot:run`. Ambas variables son obligatorias; `PORT` es opcional (8080).
Health: `/actuator/health`; Swagger: `/swagger-ui.html`.

Los tests arrancan MongoDB real mediante Flapdoodle en un puerto libre y lo detienen al finalizar.
La primera ejecución necesita descargar el binario de MongoDB 7.0.14.
La versión del binario se configura en `src/test/resources/application.yml`.
