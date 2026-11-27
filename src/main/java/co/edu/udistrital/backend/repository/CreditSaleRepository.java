package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CreditSaleRepository {
    private final MongoTemplate mongo;

    public CreditSaleRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public List<Document> findConfirmedByCashDayAndDay(ObjectId cashDayId, String day) {
        return mongo.getCollection("ventas").find(and(eq("cajaDiaId", cashDayId), eq("dia", day),
                eq("metodo", "fiado"), eq("estado", "confirmada"))).into(new java.util.ArrayList<>());
    }
}
