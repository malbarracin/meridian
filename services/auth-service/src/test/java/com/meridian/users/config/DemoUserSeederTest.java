package com.meridian.users.config;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.meridian.users.user.entity.User;
import com.meridian.users.user.repository.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DemoUserSeederTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DemoUserSeeder.class, SecurityConfig.class)
            .withBean(UserRepository.class, () -> userRepository);

    @ParameterizedTest
    @ValueSource(strings = {"dev", "docker", "test"})
    @DisplayName("Los perfiles no productivos crean el demo con BCrypt y no lo duplican al reiniciar")
    void creaDemoUnaSolaVez(String profile) {
        when(userRepository.findByEmail("demo@example.com"))
                .thenReturn(Optional.empty(), Optional.of(new User()));

        contextRunner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
            DemoUserSeeder seeder = context.getBean(DemoUserSeeder.class);
            seeder.run(new DefaultApplicationArguments());
            seeder.run(new DefaultApplicationArguments());

            ArgumentCaptor<User> captured = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captured.capture());
            verify(userRepository, times(2)).findByEmail("demo@example.com");
            User user = captured.getValue();
            assertThat(user.getEmail()).isEqualTo("demo@example.com");
            assertThat(user.getName()).isEqualTo("Demo");
            assertThat(user.getPasswordHash()).startsWith("$2a$").isNotEqualTo("DemoPass123!");
            assertThat(context.getBean(PasswordEncoder.class).matches("DemoPass123!", user.getPasswordHash())).isTrue();
            assertThat(user.getCreatedAt()).isNotNull().isEqualTo(user.getUpdatedAt());
        });
    }

    @Test
    @DisplayName("Las variables de entorno configuran las credenciales y el email se normaliza")
    void configuraCredencialesDesdeEntorno() {
        contextRunner.withPropertyValues("spring.profiles.active=dev", "SEED_DEMO_EMAIL=  ADA@EXAMPLE.COM  ",
                "SEED_DEMO_PASSWORD=CustomPass123!").run(context -> {
                    context.getBean(DemoUserSeeder.class).run(new DefaultApplicationArguments());

                    ArgumentCaptor<User> captured = ArgumentCaptor.forClass(User.class);
                    verify(userRepository).save(captured.capture());
                    assertThat(captured.getValue().getEmail()).isEqualTo("ada@example.com");
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

        contextRunner.withPropertyValues("spring.profiles.active=test").run(context -> {
            context.getBean(DemoUserSeeder.class).run(new DefaultApplicationArguments());

            verify(userRepository, times(0)).save(any(User.class));
        });
    }
}
