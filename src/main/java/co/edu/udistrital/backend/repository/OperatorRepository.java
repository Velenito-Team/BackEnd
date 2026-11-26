package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OperatorRepository {
    private final MongoTemplate mongo;
    public OperatorRepository(MongoTemplate mongo) { this.mongo = mongo; }
    public Document findActive(String username) {
        return mongo.getCollection("usuarios").find(and(eq("usuarioNormalizado", username), eq("activo", true))).first();
    }
}
