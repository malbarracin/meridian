package com.meridian.users.user.repository;

import java.time.Instant;
import java.util.Optional;

import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.meridian.users.user.entity.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataMongoTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserRepositoryTest {

    private static TransitionWalker.ReachedState<RunningMongodProcess> mongo;

    @Autowired
    private UserRepository userRepository;

    @DynamicPropertySource
    static void configureMongo(DynamicPropertyRegistry registry) {
        var configuration = new YamlPropertiesFactoryBean();
        configuration.setResources(new ClassPathResource("application.yml"));
        String version = configuration.getObject().getProperty("de.flapdoodle.mongodb.embedded.version");
        mongo = Mongod.instance().start(Version.valueOf("V" + version.replace('.', '_')));
        var address = mongo.current().getServerAddress();
        registry.add("spring.data.mongodb.uri",
                () -> "mongodb://" + address.getHost() + ":" + address.getPort());
        registry.add("spring.data.mongodb.database", () -> "auth_service_repo_test");
    }

    @AfterAll
    static void stopMongo() {
        if (mongo != null) {
            mongo.close();
        }
    }

    @BeforeEach
    void cleanUsers() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("findByEmail devuelve el usuario cuando el email existe")
    void findByEmailDevuelveUsuarioCuandoExiste() {
        User saved = userRepository.save(newUser("ada@example.com", "Ada"));

        Optional<User> found = userRepository.findByEmail("ada@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getName()).isEqualTo("Ada");
        assertThat(found.get().getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    @DisplayName("findByEmail devuelve Optional.empty cuando el email no existe")
    void findByEmailDevuelveVacioCuandoNoExiste() {
        Optional<User> found = userRepository.findByEmail("nadie@example.com");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("guardar dos usuarios con el mismo email lanza DuplicateKeyException")
    void guardarEmailDuplicadoLanzaDuplicateKeyException() {
        userRepository.save(newUser("ada@example.com", "Ada"));

        assertThatThrownBy(() -> userRepository.save(newUser("ada@example.com", "Otra")))
                .isInstanceOf(DuplicateKeyException.class);

        assertThat(userRepository.findAll()).hasSize(1);
    }

    private static User newUser(String email, String name) {
        Instant now = Instant.parse("2026-01-15T10:00:00Z");
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$hashed");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return user;
    }
}
