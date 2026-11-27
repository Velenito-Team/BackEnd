package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.OperatorRepository;
import java.util.Locale;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperatorService {
    private final OperatorRepository repository;
    private final String username;

    public OperatorService(OperatorRepository repository, @Value("${velenito.pos.user:}") String username) {
        this.repository = repository;
        this.username = username;
    }

    public Document current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authenticatedUsername = authenticatedUsername(authentication);
        String currentUsername = authenticatedUsername == null ? username : authenticatedUsername;
        var user = currentUsername == null || currentUsername.isBlank()
                ? null
                : repository.findActive(currentUsername.trim().toLowerCase(Locale.ROOT));
        if (user == null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configura un operador activo para la caja.");
        return user;
    }

    private static String authenticatedUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())
                || authentication.getName() == null || authentication.getName().isBlank())
            return null;
        return authentication.getName();
    }

    public ObjectId id() {
        return current().getObjectId("_id");
    }
}
