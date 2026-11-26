package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;

import com.mongodb.client.MongoClient;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class InventoryRepository {
    private final MongoClient client;
    private final MongoTemplate mongo;
    public InventoryRepository(MongoClient client, MongoTemplate mongo) { this.client = client; this.mongo = mongo; }

    public String receive(ObjectId productId, BigDecimal quantity, BigDecimal cost, ObjectId actor, UUID key, String hash) {
        ObjectId receptionId = new ObjectId();
        Date now = new Date();
        try (var session = client.startSession()) {
            return session.withTransaction(() -> {
                var operations = mongo.getCollection("operaciones");
                Document existing = operations.find(session, and(eq("por", actor), eq("tipo", "recepcion"), eq("clave", key.toString()))).first();
                if (existing != null) {
                    if (!hash.equals(existing.getString("solicitudHash"))) throw new IllegalArgumentException("La clave pertenece a otra recepción.");
                    return existing.getObjectId("resultadoId").toHexString();
                }
                Document product = mongo.getCollection("productos").find(session, eq("_id", productId)).first();
                if (product == null) throw new IllegalArgumentException("El producto no existe.");
                if (!product.getBoolean("fraccionable", false) && quantity.stripTrailingZeros().scale() > 0)
                    throw new IllegalArgumentException("Este producto requiere cantidades enteras.");
                mongo.getCollection("productos").updateOne(session, eq("_id", productId),
                        combine(inc("stock", new Decimal128(quantity)), set("actualizadoEn", now)));
                Document line = new Document("productoId", productId).append("nombre", product.getString("nombre"))
                        .append("unidad", product.getString("unidad")).append("cantidad", new Decimal128(quantity))
                        .append("costoUnitario", new Decimal128(cost));
                mongo.getCollection("recepciones").insertOne(session, new Document("_id", receptionId)
                        .append("lineas", List.of(line)).append("por", actor).append("fecha", now));
                operations.insertOne(session, new Document("por", actor).append("tipo", "recepcion")
                        .append("clave", key.toString()).append("solicitudHash", hash)
                        .append("resultadoId", receptionId).append("fecha", now));
                return receptionId.toHexString();
            });
        }
    }
}
