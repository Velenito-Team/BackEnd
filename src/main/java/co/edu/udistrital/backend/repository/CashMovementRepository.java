package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CashMovementRepository {
    private final MongoTemplate mongo;

    public CashMovementRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public List<Document> findByCashDayAndDay(ObjectId cashDayId, String day) {
        return mongo.getCollection("movimientosCaja")
                .find(and(eq("cajaDiaId", cashDayId), eq("dia", day))).into(new java.util.ArrayList<>());
    }
}
