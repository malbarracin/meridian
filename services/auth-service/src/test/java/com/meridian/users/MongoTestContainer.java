package com.meridian.users;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base reutilizable para tests Spring con MongoDB real; requiere Docker disponible.
 * Los tests concretos agregan {@code @SpringBootTest} o {@code @DataMongoTest} y limpian sus datos.
 * El contenedor se comparte durante la JVM y Testcontainers lo elimina al finalizar.
 * No combinar con {@code @Testcontainers}/{@code @Container}: detenerlo entre clases invalidaría
 * las conexiones de los contextos Spring cacheados.
 */
public abstract class MongoTestContainer {

    private static final MongoDBContainer MONGO = new MongoDBContainer(DockerImageName.parse("mongo:7.0.14"))
            .withReuse(false);

    static {
        MONGO.start();
    }

    /** @return contenedor compartido para simular interrupciones recuperables en los tests */
    protected static MongoDBContainer mongoContainer() {
        return MONGO;
    }

    /**
     * Registra la URI del replica set antes de inicializar la conexión de Spring Data MongoDB.
     *
     * @param registry propiedades dinámicas del contexto Spring del test
     */
    @DynamicPropertySource
    protected static void registerMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }
}
