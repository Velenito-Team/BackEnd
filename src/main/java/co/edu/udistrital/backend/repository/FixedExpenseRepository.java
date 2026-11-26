package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.ascending;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class FixedExpenseRepository {
    private final MongoTemplate mongo;

    public FixedExpenseRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public Document save(String concept, String normalizedConcept, String month, long amount,
            int reminderDay, ObjectId actor, Date createdAt) {
        Document expense = new Document("_id", new ObjectId()).append("concepto", concept)
                .append("conceptoNormalizado", normalizedConcept).append("mes", month)
                .append("monto", amount).append("diaRecordatorio", reminderDay)
                .append("por", actor).append("fecha", createdAt);
        mongo.getCollection("gastosFijos").insertOne(expense);
        return expense;
    }

    public List<Document> findByMonth(String month) {
        return mongo.getCollection("gastosFijos").find(eq("mes", month))
                .sort(ascending("conceptoNormalizado")).into(new ArrayList<>());
    }
}
