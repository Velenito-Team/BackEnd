package co.edu.udistrital.backend.service;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import co.edu.udistrital.backend.repository.OperatorRepository;

@Service
public class OperatorService {
    private final OperatorRepository repository;
    private final String username;

    public OperatorService(OperatorRepository repository, @Value("${velenito.pos.user:}") String username) {
        this.repository = repository;
        this.username = username;
    }

    public Document current() {
        var user = repository.findActive(username);
        if (user == null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configura un operador activo para la caja.");
        return user;
    }

    public ObjectId id() {
        return current().getObjectId("_id");
    }
}
