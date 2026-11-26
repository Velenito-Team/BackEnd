package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.descending;

import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CashClosingRepository {
    private static final String COLLECTION = "cierresCaja";
    private final MongoTemplate mongo;

    public CashClosingRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public void save(Document closing) { mongo.getCollection(COLLECTION).insertOne(closing); }

    public Optional<Document> findById(ObjectId id) {
        return Optional.ofNullable(mongo.getCollection(COLLECTION).find(eq("_id", id)).first());
    }

    public Optional<Document> findLatestByCashDayId(ObjectId cashDayId) {
        return Optional.ofNullable(mongo.getCollection(COLLECTION).find(eq("cajaDiaId", cashDayId))
                .sort(descending("revision")).first());
    }
}
