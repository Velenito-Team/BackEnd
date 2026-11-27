package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.repository.OperatorRepository;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class OperatorServiceTest {
    private final OperatorRepository repository = mock(OperatorRepository.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void prefersAuthenticatedSessionUsernameToConfiguredFallback() {
        Document sessionUser = new Document("_id", "session-user");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("Tendero", "", java.util.List.of()));
        when(repository.findActive("tendero")).thenReturn(sessionUser);
        OperatorService service = new OperatorService(repository, "configured-user");

        assertSame(sessionUser, service.current());
        verify(repository).findActive("tendero");
    }

    @Test
    void usesConfiguredUsernameWithoutAuthenticatedSession() {
        Document configuredUser = new Document("_id", "configured-user");
        when(repository.findActive("configured-user")).thenReturn(configuredUser);
        OperatorService service = new OperatorService(repository, "Configured-User");

        assertSame(configuredUser, service.current());
        verify(repository).findActive("configured-user");
    }
}
