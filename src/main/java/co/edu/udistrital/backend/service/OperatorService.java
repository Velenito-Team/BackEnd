package co.edu.udistrital.backend.service;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OperatorService {
    private final MongoTemplate mongo;
    private final String username;

    public OperatorService(MongoTemplate mongo, @Value("${velenito.pos.user:}") String username) {
        this.mongo = mongo;
        this.username = username;
    }

    public Document current() {
        var user = mongo.getCollection("usuarios").find(new Document("usuarioNormalizado", username)
                .append("activo", true)).first();
        if (user == null)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configura un operador activo para la caja.");
        return user;
    }

    public ObjectId id() {
        return current().getObjectId("_id");
    }
}
