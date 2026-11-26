package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.model.Role;
import co.edu.udistrital.backend.model.User;
import co.edu.udistrital.backend.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

class UserServiceTest {
    private UserRepository repository;
    private PasswordEncoder encoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        encoder = mock(PasswordEncoder.class);
        service = new UserService(repository, encoder);
        when(encoder.encode("secreto123")).thenReturn("hash");
    }

    @Test
    void creaUsuarioConRolYPermiteSuEstadoActivo() {
        when(repository.findByUsuarioNormalizado("cajero1")).thenReturn(Optional.empty());
        User saved = user(Role.cajero, true);
        when(repository.save(any(User.class))).thenReturn(saved);

        UserService.UserView result = service.create(
                new UserService.CreateRequest("Cajero", "cajero1", "secreto123", Role.cajero));

        assertEquals(Role.cajero, result.rol());
        assertEquals(true, result.activo());
    }

    @Test
    void impideDesactivarAlUltimoAdministrador() {
        User admin = user(Role.administrador, true);
        when(repository.findById("admin-id")).thenReturn(Optional.of(admin));
        when(repository.countByRolAndActivoTrue(Role.administrador)).thenReturn(1L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.setActive("admin-id", false));

        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void impideRetirarRolAlUltimoAdministrador() {
        User admin = user(Role.administrador, true);
        when(repository.findById("admin-id")).thenReturn(Optional.of(admin));
        when(repository.countByRolAndActivoTrue(Role.administrador)).thenReturn(1L);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.setRole("admin-id", Role.cajero));

        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void permiteDesactivarSiExisteOtroAdministrador() {
        User admin = user(Role.administrador, true);
        when(repository.findById("admin-id")).thenReturn(Optional.of(admin));
        when(repository.countByRolAndActivoTrue(Role.administrador)).thenReturn(2L);
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserService.UserView result = service.setActive("admin-id", false);

        assertEquals(false, result.activo());
    }

    private static User user(Role role, boolean active) {
        User user = new User("Nombre", "usuario", "hash", role);
        user.setId("admin-id");
        user.setActivo(active);
        return user;
    }
}
