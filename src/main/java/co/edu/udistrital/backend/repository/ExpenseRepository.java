package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.Date;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ExpenseRepository {
    private final MongoTemplate mongo;

    public ExpenseRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public Optional<Document> findOpenCashDay(String register, String day) {
        return Optional.ofNullable(mongo.getCollection("cajasDia")
                .find(and(eq("caja", register), eq("dia", day), eq("estado", "abierta"))).first());
    }

    public Document save(ObjectId cashDayId, String day, ObjectId actor, String paymentMethod,
            long amount, String description, ObjectId providerId, Date date) {
        Document movement = new Document("_id", new ObjectId()).append("cajaDiaId", cashDayId)
                .append("dia", day).append("por", actor).append("fecha", date)
                .append("tipo", "egreso").append("medio", paymentMethod).append("monto", amount)
                .append("descripcion", description);
        if (providerId != null) movement.append("proveedorId", providerId);
        mongo.getCollection("movimientosCaja").insertOne(movement);
        return movement;
    }
}
