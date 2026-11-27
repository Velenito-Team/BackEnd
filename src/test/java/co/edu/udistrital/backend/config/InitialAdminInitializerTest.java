package co.edu.udistrital.backend.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.model.Role;
import co.edu.udistrital.backend.model.User;
import co.edu.udistrital.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class InitialAdminInitializerTest {
    private final UserRepository users = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final InitialAdminInitializer initializer =
            new InitialAdminInitializer(users, passwordEncoder, "", "admin1234");

    @Test
    void createsDefaultAdministratorOnlyWhenUserCollectionIsEmpty() throws Exception {
        when(users.count()).thenReturn(0L);
        when(passwordEncoder.encode("admin1234")).thenReturn("encoded-password");
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        initializer.run(mock(ApplicationArguments.class));

        var saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals("admin", saved.getValue().getUsuarioNormalizado());
        assertEquals(Role.administrador, saved.getValue().getRol());
        assertEquals("encoded-password", saved.getValue().getContrasenaHash());
    }

    @Test
    void leavesExistingUsersUntouched() throws Exception {
        when(users.count()).thenReturn(1L);

        initializer.run(mock(ApplicationArguments.class));

        verify(users, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }
}
