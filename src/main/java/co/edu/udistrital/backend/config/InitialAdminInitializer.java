package co.edu.udistrital.backend.config;

import co.edu.udistrital.backend.model.Role;
import co.edu.udistrital.backend.model.User;
import co.edu.udistrital.backend.repository.UserRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final boolean seedDefaultUsers;

    @Autowired
    public InitialAdminInitializer(UserRepository users, PasswordEncoder passwordEncoder,
            @Value("${velenito.pos.user:}") String configuredUsername,
            @Value("${velenito.pos.bootstrap-password:admin1234}") String bootstrapPassword,
            @Value("${velenito.pos.seed-default-users:false}") boolean seedDefaultUsers) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.configuredUsername = configuredUsername;
        this.bootstrapPassword = bootstrapPassword;
        this.seedDefaultUsers = seedDefaultUsers;
    }

    public InitialAdminInitializer(UserRepository users, PasswordEncoder passwordEncoder,
            String configuredUsername, String bootstrapPassword) {
        this(users, passwordEncoder, configuredUsername, bootstrapPassword, false);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() == 0) {
            String username = configuredUsername == null || configuredUsername.isBlank()
                    ? "admin"
                    : configuredUsername.trim();
            if (bootstrapPassword == null || bootstrapPassword.length() < 8)
                throw new IllegalStateException("La contrasena inicial debe tener al menos 8 caracteres.");
            users.save(new User("Administrador", username, passwordEncoder.encode(bootstrapPassword),
                    Role.administrador));
        }

        if (seedDefaultUsers) {
            ensureUser("Administrador", "admin", "Admin1234!", Role.administrador);
            ensureUser("Tendero", "tendero", "Tendero1234!", Role.tendero);
            ensureUser("Cajero", "cajero", "Cajero1234!", Role.cajero);
        }
    }

    private void ensureUser(String name, String username, String password, Role role) {
        if (users.findByUsuarioNormalizado(username.toLowerCase(Locale.ROOT)).isEmpty()) {
            users.save(new User(name, username, passwordEncoder.encode(password), role));
        }
    }
}
