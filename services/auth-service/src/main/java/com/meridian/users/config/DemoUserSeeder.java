package com.meridian.users.config;

import java.time.Instant;
import java.util.Locale;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.meridian.users.user.entity.User;
import com.meridian.users.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Crea el usuario demo solo en dev, docker o test; prod siempre lo desactiva, incluso con perfiles combinados.
 * Sin esos perfiles el seed es opcional y permanece desactivado.
 * Las variables SEED_DEMO_EMAIL y SEED_DEMO_PASSWORD permiten configurar las credenciales;
 * el email por defecto es demo@example.com y la contraseña es obligatoria, sin valor por defecto.
 * Un usuario existente conserva sus credenciales y datos en cada reinicio.
 */
@Component
@Profile("(dev | docker | test) & !prod")
@RequiredArgsConstructor
public class DemoUserSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    /** @param args argumentos de arranque; la carga se configura mediante el entorno de Spring */
    @Override
    public void run(ApplicationArguments args) {
        String password = environment.getRequiredProperty("SEED_DEMO_PASSWORD");
        if (password.isBlank()) {
            throw new IllegalStateException("SEED_DEMO_PASSWORD debe contener una contraseña no vacía");
        }
        String email = environment.getProperty("SEED_DEMO_EMAIL", "demo@example.com")
                .trim().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        Instant now = Instant.now();
        User user = new User();
        user.setName("Demo");
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        userRepository.save(user);
    }
}
