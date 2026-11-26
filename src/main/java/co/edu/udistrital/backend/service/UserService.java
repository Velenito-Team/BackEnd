package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.model.Role;
import co.edu.udistrital.backend.model.User;
import co.edu.udistrital.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public record CreateRequest(String nombre, String usuario, String contrasena, Role rol) { }
    public record UserView(String id, String nombre, String usuario, Role rol) { }

    public UserView create(CreateRequest request) {
        if (request == null || blank(request.nombre()) || blank(request.usuario())
                || request.contrasena() == null || request.contrasena().length() < 8 || request.rol() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Indica nombre, usuario, rol y una contraseña de al menos 8 caracteres.");

        String username = request.usuario().trim();
        String normalized = username.toLowerCase(java.util.Locale.ROOT);
        if (repository.findByUsuarioNormalizado(normalized).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese usuario ya está registrado.");

        User user = repository.save(new User(request.nombre().trim(), username,
                passwordEncoder.encode(request.contrasena()), request.rol()));
        return toView(user);
    }

    public UserView findByUsername(String username) {
        return repository.findByUsuarioNormalizado(username)
                .map(UserService::toView)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "La sesión ya no es válida."));
    }

    private static UserView toView(User user) {
        return new UserView(user.getId(), user.getNombre(), user.getUsuario(), user.getRol());
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
