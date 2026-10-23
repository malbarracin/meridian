package com.meridian.users;

import java.time.Duration;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.meridian.users.user.entity.User;
import com.meridian.users.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Completa H2.3 con registro HTTP, seed explícito y recuperación tras una interrupción real de MongoDB. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthIntegrationTest extends MongoTestContainer {

    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String EMAIL = "usuario@ejemplo.com";
    private static final String NAME = "Usuario Ejemplo";
    private static final String PASSWORD = "miPassword123";

    private final MockMvc mockMvc;

    private final ObjectMapper objectMapper;

    private final UserRepository userRepository;

    private final MongoTemplate mongoTemplate;

    @Autowired
    AuthIntegrationTest(MockMvc mockMvc, ObjectMapper objectMapper,
            UserRepository userRepository, MongoTemplate mongoTemplate) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @BeforeEach
    void limpiaUsuarios() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("El registro HTTP persiste todos los campos del usuario en MongoDB real")
    void registroPersisteUsuario() throws Exception {
        JsonNode response = registraUsuario("Usuario@Ejemplo.COM");

        User persisted = userRepository.findById(response.path("user").path("id").asText()).orElseThrow();
        assertThat(persisted.getId()).isNotBlank();
        assertThat(persisted.getName()).isEqualTo(NAME);
        assertThat(persisted.getEmail()).isEqualTo(EMAIL);
        assertThat(persisted.getPasswordHash()).isNotBlank();
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getUpdatedAt()).isNotNull().isEqualTo(persisted.getCreatedAt());
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Un segundo registro con el mismo email devuelve 409 y no persiste otro usuario")
    void registroEmailDuplicadoDevuelve409() throws Exception {
        registraUsuario(EMAIL);

        verificaDuplicado(EMAIL);
    }

    @Test
    @DisplayName("Un segundo registro con distintas mayúsculas devuelve 409 sin duplicar documentos")
    void registroEmailDuplicadoConMayusculasDevuelve409() throws Exception {
        registraUsuario(EMAIL);

        verificaDuplicado("USUARIO@EJEMPLO.COM");
    }

    @Test
    @DisplayName("El documento guarda un hash BCrypt verificable y nunca el password en plano")
    void registroGuardaPasswordHasheado() throws Exception {
        registraUsuario(EMAIL);

        Document persisted = mongoTemplate.getCollection("users").find(new Document("email", EMAIL)).first();
        assertThat(persisted).isNotNull();
        String passwordHash = persisted.getString("passwordHash");
        assertThat(passwordHash).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(new BCryptPasswordEncoder().matches(PASSWORD, passwordHash)).isTrue();
        assertThat(persisted.toJson()).doesNotContain(PASSWORD);
        assertThat(persisted).doesNotContainKey("password");
    }

    @Test
    @DisplayName("Ejecutar dos veces el mismo seed con upsert no genera errores ni duplicados")
    void seedEsIdempotente() {
        Query query = Query.query(Criteria.where("email").is(EMAIL));
        Update seed = new Update().setOnInsert("name", NAME).setOnInsert("email", EMAIL)
                .setOnInsert("passwordHash", new BCryptPasswordEncoder().encode(PASSWORD));

        mongoTemplate.upsert(query, seed, User.class);
        mongoTemplate.upsert(query, seed, User.class);

        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(userRepository.findByEmail(EMAIL)).hasValueSatisfying(user -> {
            assertThat(user.getName()).isEqualTo(NAME);
            assertThat(new BCryptPasswordEncoder().matches(PASSWORD, user.getPasswordHash())).isTrue();
        });
    }

    @Test
    @DisplayName("Tras interrumpir MongoDB el perfil persistido vuelve a estar disponible por HTTP")
    void persistenciaDisponibleTrasReconexion() throws Exception {
        JsonNode registered = registraUsuario(EMAIL);
        String userId = registered.path("user").path("id").asText();
        String token = registered.path("accessToken").asText();
        var container = mongoContainer();
        String uri = container.getReplicaSetUrl();
        String separator = uri.contains("?") ? "&" : "?";
        // Un cliente acotado permite observar el fallo sin esperar el timeout de producción.
        try (MongoClient probe = MongoClients.create(uri + separator
                + "serverSelectionTimeoutMS=1000&socketTimeoutMS=1000&connectTimeoutMS=1000")) {
            assertThat(probe.getDatabase("test").runCommand(new Document("ping", 1)).getDouble("ok"))
                    .isEqualTo(1.0);
            container.getDockerClient().pauseContainerCmd(container.getContainerId()).exec();
            try {
                assertThatThrownBy(() -> probe.getDatabase("test").runCommand(new Document("ping", 1)))
                        .isInstanceOf(MongoException.class);
            } finally {
                container.getDockerClient().unpauseContainerCmd(container.getContainerId()).exec();
            }
        }

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.email").value(EMAIL));
            assertThat(userRepository.findById(userId)).isPresent();
        });
        registraUsuario("otro@ejemplo.com");
        assertThat(userRepository.count()).isEqualTo(2);
    }

    private JsonNode registraUsuario(String email) throws Exception {
        String response = mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content(registrationBody(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.id").isNotEmpty())
                .andExpect(jsonPath("$.user.name").value(NAME))
                .andExpect(jsonPath("$.user.email").value(email.trim().toLowerCase()))
                .andExpect(jsonPath("$.user.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode body = objectMapper.readTree(response);
        assertThat(body.path("expiresIn").asLong()).isPositive();
        return body;
    }

    private void verificaDuplicado(String email) throws Exception {
        mockMvc.perform(post(REGISTER_PATH).contentType(MediaType.APPLICATION_JSON).content(registrationBody(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
        assertThat(userRepository.count()).isEqualTo(1);
    }

    private String registrationBody(String email) throws Exception {
        return objectMapper.writeValueAsString(Map.of("name", NAME, "email", email, "password", PASSWORD));
    }
}
