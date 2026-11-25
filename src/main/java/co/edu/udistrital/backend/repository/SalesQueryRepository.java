package co.edu.udistrital.backend.repository;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SalesQueryRepository {
    private final MongoTemplate mongo;

    public SalesQueryRepository(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public List<Document> between(LocalDate from, LocalDate to, String method, String status, String productId) {
        if (to.isBefore(from) || to.isAfter(from.plusYears(1))) {
            throw new IllegalArgumentException("El período debe tener como máximo un año.");
        }
        var zone = ZoneId.of("America/Bogota");
        var filter = new Document("fecha", new Document("$gte", Date.from(from.atStartOfDay(zone).toInstant()))
                .append("$lt", Date.from(to.plusDays(1).atStartOfDay(zone).toInstant())));
        if (method != null && !method.isBlank())
            filter.append("metodo", method);
        if (status != null && !status.isBlank())
            filter.append("estado", status);
        if (productId != null && !productId.isBlank())
            filter.append("lineas.productoId", new org.bson.types.ObjectId(productId));
        return mongo.getCollection("ventas").find(filter).sort(new Document("fecha", -1).append("_id", -1))
                .into(new ArrayList<>());
    }
}
