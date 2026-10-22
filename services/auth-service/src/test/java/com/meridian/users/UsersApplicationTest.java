package com.meridian.users;

import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.mongo.transitions.Mongod;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.reverse.TransitionWalker;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UsersApplicationTest {

    private static TransitionWalker.ReachedState<RunningMongodProcess> mongo;

    @Autowired
    private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void configureMongo(DynamicPropertyRegistry registry) {
        var configuration = new YamlPropertiesFactoryBean();
        configuration.setResources(new ClassPathResource("application.yml"));
        String version = configuration.getObject().getProperty("de.flapdoodle.mongodb.embedded.version");
        mongo = Mongod.instance().start(Version.valueOf("V" + version.replace('.', '_')));
        var address = mongo.current().getServerAddress();
        registry.add("spring.data.mongodb.uri",
                () -> "mongodb://" + address.getHost() + ":" + address.getPort());
        registry.add("spring.data.mongodb.database", () -> "auth_service_test");
    }

    @AfterAll
    static void stopMongo() {
        if (mongo != null) {
            mongo.close();
        }
    }

    @Test
    @DisplayName("El contexto arranca y conecta con MongoDB embebido")
    void contextLoads() {
        Document response = mongoTemplate.executeCommand(new Document("ping", 1));
        assertThat(response.getDouble("ok")).isEqualTo(1.0);
        assertThat(mongoTemplate.getDb().getName()).isEqualTo("auth_service_test");
    }
}
