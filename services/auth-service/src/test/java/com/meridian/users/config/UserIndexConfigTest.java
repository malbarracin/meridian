package com.meridian.users.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;

import com.meridian.users.user.entity.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserIndexConfigTest {

    private final MongoTemplate mongoTemplate = mock(MongoTemplate.class);
    private final IndexOperations indexOperations = mock(IndexOperations.class);
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(UserIndexConfig.class)
            .withPropertyValues("spring.profiles.active=prod")
            .withBean(MongoTemplate.class, () -> mongoTemplate);

    @Test
    @DisplayName("Producción garantiza el índice único de email durante el arranque")
    void garantizaIndiceUnicoAntesDeCompletarArranque() {
        when(mongoTemplate.indexOps(User.class)).thenReturn(indexOperations);

        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            ArgumentCaptor<IndexDefinition> index = ArgumentCaptor.forClass(IndexDefinition.class);
            verify(indexOperations).createIndex(index.capture());
            assertThat(index.getValue().getIndexKeys()).containsEntry("email", 1);
            assertThat(index.getValue().getIndexOptions()).containsEntry("unique", true);
        });
    }

    @Test
    @DisplayName("Producción rechaza el arranque si no puede garantizar el índice único")
    void rechazaArranqueSinIndiceUnico() {
        when(mongoTemplate.indexOps(User.class)).thenReturn(indexOperations);
        when(indexOperations.createIndex(any(IndexDefinition.class)))
                .thenThrow(new DataIntegrityViolationException("Índice incompatible"));

        contextRunner.run(context -> assertThat(context).hasFailed()
                .getFailure().hasRootCauseInstanceOf(DataIntegrityViolationException.class));
    }
}
