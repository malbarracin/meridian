package com.meridian.users.config;

import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.meridian.users.user.entity.User;
import com.meridian.users.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"SEED_DEMO_EMAIL=seed@example.com", "SEED_DEMO_PASSWORD=SeedPass123!"})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoUserSeederIntegrationTest {

    private static TransitionWalker.ReachedState<RunningMongodProcess> mongo;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DemoUserSeeder seeder;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @DynamicPropertySource
    static void configureMongo(DynamicPropertyRegistry registry) {
        mongo = Mongod.instance().start(Version.V7_0_14);
        var address = mongo.current().getServerAddress();
        registry.add("spring.data.mongodb.uri",
                () -> "mongodb://" + address.getHost() + ":" + address.getPort());
        registry.add("spring.data.mongodb.database", () -> "auth_service_seed_test");
    }

    @AfterAll
    static void stopMongo() {
        if (mongo != null) {
            mongo.close();
        }
    }

    @Test
    @DisplayName("El arranque crea el demo en MongoDB y repetir el seed conserva el documento y su hash")
    void creaDemoAlArrancarYConservaDocumentoAlReiniciar() {
        User initial = userRepository.findByEmail("seed@example.com").orElseThrow();
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(initial.getPasswordHash()).startsWith("$2a$").isNotEqualTo("SeedPass123!");
        assertThat(passwordEncoder.matches("SeedPass123!", initial.getPasswordHash())).isTrue();

        seeder.run(new DefaultApplicationArguments());
        seeder.run(new DefaultApplicationArguments());

        User afterRestart = userRepository.findByEmail("seed@example.com").orElseThrow();
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(afterRestart.getId()).isEqualTo(initial.getId());
        assertThat(afterRestart.getPasswordHash()).isEqualTo(initial.getPasswordHash());
        assertThat(afterRestart.getUpdatedAt()).isEqualTo(initial.getUpdatedAt());
    }
}
