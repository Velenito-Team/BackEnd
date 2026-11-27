package co.edu.udistrital.backend.config;

import co.edu.udistrital.backend.model.Role;
import co.edu.udistrital.backend.model.User;
import co.edu.udistrital.backend.repository.UserRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class InitialAdminInitializer implements ApplicationRunner {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String configuredUsername;
    private final String bootstrapPassword;

    public InitialAdminInitializer(UserRepository users, PasswordEncoder passwordEncoder,
            @Value("${velenito.pos.user:}") String configuredUsername,
            @Value("${velenito.pos.bootstrap-password:admin1234}") String bootstrapPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.configuredUsername = configuredUsername;
        this.bootstrapPassword = bootstrapPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() != 0)
            return;

        String username = configuredUsername == null || configuredUsername.isBlank()
                ? "admin"
                : configuredUsername.trim();
        if (bootstrapPassword == null || bootstrapPassword.length() < 8)
            throw new IllegalStateException("La contraseña inicial debe tener al menos 8 caracteres.");
        User administrator = new User("Administrador", username, passwordEncoder.encode(bootstrapPassword),
                Role.administrador);
        users.save(administrator);
    }
}
