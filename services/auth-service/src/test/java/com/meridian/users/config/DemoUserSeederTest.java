package com.meridian.users.config;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.meridian.users.user.entity.User;
import com.meridian.users.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DemoUserSeederTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DemoUserSeeder.class, SecurityConfig.class)
            .withBean(UserRepository.class, () -> userRepository);

    @BeforeEach
    void resetRepository() {
        reset(userRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "docker", "test"})
    @DisplayName("Los perfiles no productivos crean el demo con BCrypt y no lo duplican al reiniciar")
    void creaDemoUnaSolaVez(String profile) {
        when(userRepository.findByEmail("demo@example.com"))
                .thenReturn(Optional.empty(), Optional.of(new User()));

        contextRunner.withPropertyValues("spring.profiles.active=" + profile,
                "SEED_DEMO_PASSWORD=ExplicitTestPass123!").run(context -> {
            DemoUserSeeder seeder = context.getBean(DemoUserSeeder.class);
            seeder.run(new DefaultApplicationArguments());
            seeder.run(new DefaultApplicationArguments());

            ArgumentCaptor<User> captured = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captured.capture());
            verify(userRepository, times(2)).findByEmail("demo@example.com");
            User user = captured.getValue();
            assertThat(user.getEmail()).isEqualTo("demo@example.com");
            assertThat(user.getName()).isEqualTo("Demo");
            assertThat(user.getPasswordHash()).startsWith("$2a$").isNotEqualTo("ExplicitTestPass123!");
            assertThat(context.getBean(PasswordEncoder.class)
                    .matches("ExplicitTestPass123!", user.getPasswordHash())).isTrue();
            assertThat(user.getCreatedAt()).isNotNull().isEqualTo(user.getUpdatedAt());
        });
    }

    @Test
    @DisplayName("Las variables de entorno configuran las credenciales y el email se normaliza")
    void configuraCredencialesDesdeEntorno() {
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.empty());

        contextRunner.withPropertyValues("spring.profiles.active=dev", "SEED_DEMO_EMAIL=  ADA@EXAMPLE.COM  ",
                "SEED_DEMO_PASSWORD=CustomPass123!").run(context -> {
                    context.getBean(DemoUserSeeder.class).run(new DefaultApplicationArguments());

                    ArgumentCaptor<User> captured = ArgumentCaptor.forClass(User.class);
                    verify(userRepository).save(captured.capture());
                    assertThat(captured.getValue().getEmail()).isEqualTo("ada@example.com");
                    assertThat(captured.getValue().getPasswordHash()).startsWith("$2a$")
                            .isNotEqualTo("CustomPass123!");
                    assertThat(context.getBean(PasswordEncoder.class)
                            .matches("CustomPass123!", captured.getValue().getPasswordHash())).isTrue();
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {"prod", "prod,dev", "prod,docker", "prod,test", "default"})
    @DisplayName("Producción y el perfil por defecto no registran ni ejecutan el seed")
    void desactivaSeedFueraDePerfilesPermitidos(String profiles) {
        contextRunner.withPropertyValues("spring.profiles.active=" + profiles).run(context -> {
            assertThat(context).doesNotHaveBean(DemoUserSeeder.class);
            verifyNoInteractions(userRepository);
        });
    }

    @Test
    @DisplayName("Un usuario existente conserva sus datos y no se vuelve a guardar")
    void conservaUsuarioExistente() {
        when(userRepository.findByEmail("demo@example.com")).thenReturn(Optional.of(new User()));

        contextRunner.withPropertyValues("spring.profiles.active=test",
                "SEED_DEMO_PASSWORD=ExplicitTestPass123!").run(context -> {
            context.getBean(DemoUserSeeder.class).run(new DefaultApplicationArguments());

            verify(userRepository, times(0)).save(any(User.class));
            verify(userRepository).findByEmail(anyString());
        });
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = {" ", "   "})
    @DisplayName("El seed rechaza contraseñas vacías antes de acceder a MongoDB")
    void rechazaPasswordVacio(String password) {
        contextRunner.withPropertyValues("spring.profiles.active=dev", "SEED_DEMO_PASSWORD=" + password)
                .run(context -> {
                    DemoUserSeeder seeder = context.getBean(DemoUserSeeder.class);
                    assertThatIllegalStateException()
                            .isThrownBy(() -> seeder.run(new DefaultApplicationArguments()))
                            .withMessageContaining("SEED_DEMO_PASSWORD");
                    verifyNoInteractions(userRepository);
                });
    }

    @Test
    @DisplayName("El seed rechaza la contraseña ausente antes de acceder a MongoDB")
    void rechazaPasswordSinConfigurar() {
        contextRunner.withPropertyValues("spring.profiles.active=dev").run(context -> {
            assertThatIllegalStateException()
                    .isThrownBy(() -> context.getBean(DemoUserSeeder.class).run(new DefaultApplicationArguments()))
                    .withMessageContaining("SEED_DEMO_PASSWORD");
            verifyNoInteractions(userRepository);
        });
    }
}
