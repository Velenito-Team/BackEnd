package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import java.util.Date;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CashDayRepository {
    private static final String COLLECTION = "cajasDia";
    private final MongoTemplate mongo;

    public CashDayRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public Optional<Document> find(String register, String day) {
        return Optional.ofNullable(mongo.getCollection(COLLECTION)
                .find(and(eq("caja", register), eq("dia", day))).first());
    }

    public Optional<Document> findById(ObjectId id) {
        return Optional.ofNullable(mongo.getCollection(COLLECTION).find(eq("_id", id)).first());
    }

    public Document create(ObjectId actor, String register, String day, Date openingDate) {
        Document document = new Document("_id", new ObjectId()).append("caja", register)
                .append("dia", day).append("estado", "abierta").append("version", 0L)
                .append("apertura", new Document("por", actor).append("fecha", openingDate).append("base", 0L));
        mongo.getCollection(COLLECTION).insertOne(document);
        return document;
    }

    public void close(ObjectId id, ObjectId closingId) {
        mongo.getCollection(COLLECTION).updateOne(eq("_id", id),
                combine(set("estado", "cerrada"), set("cierreVigenteId", closingId), inc("version", 1)));
    }

    public void pointToClosing(ObjectId id, ObjectId closingId) {
        mongo.getCollection(COLLECTION).updateOne(eq("_id", id), set("cierreVigenteId", closingId));
    }
}
