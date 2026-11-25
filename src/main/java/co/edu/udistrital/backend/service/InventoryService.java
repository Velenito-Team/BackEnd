package co.edu.udistrital.backend.service;

import com.mongodb.client.MongoClient;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

@Service
public class InventoryService {
    private final MongoClient client;
    private final MongoTemplate mongo;
    private final OperatorService operator;

    public InventoryService(MongoClient client, MongoTemplate mongo, OperatorService operator) {
        this.client = client;
        this.mongo = mongo;
        this.operator = operator;
    }

    public record Entry(String productoId, BigDecimal cantidad, BigDecimal costoUnitario) {
    }

    public String receive(Entry entry, UUID key) {
        if (entry == null || entry.productoId() == null || !ObjectId.isValid(entry.productoId())
                || entry.cantidad() == null || entry.cantidad().signum() <= 0
                || entry.cantidad().stripTrailingZeros().scale() > 3 || entry.costoUnitario() == null
                || entry.costoUnitario().signum() <= 0)
            throw invalid("Indica cantidad y costo positivos.");
        var id = new ObjectId(entry.productoId());
        var actor = operator.id();
        var filter = and(eq("por", actor), eq("tipo", "recepcion"), eq("clave", key.toString()));
        var hash = SaleService.sha256(
                id + ":" + entry.cantidad().stripTrailingZeros() + ":" + entry.costoUnitario().stripTrailingZeros());
        var db = mongo.getDb();
        var receptionId = new ObjectId();
        var now = new Date();
        try (var session = client.startSession()) {
            return session.withTransaction(() -> {
                var existing = db.getCollection("operaciones").find(session, filter).first();
                if (existing != null) {
                    if (!hash.equals(existing.getString("solicitudHash")))
                        throw invalid("La clave pertenece a otra recepción.");
                    return existing.getObjectId("resultadoId").toHexString();
                }
                var product = db.getCollection("productos").find(session, eq("_id", id)).first();
                if (product == null)
                    throw invalid("El producto no existe.");
                if (!product.getBoolean("fraccionable", false) && entry.cantidad().stripTrailingZeros().scale() > 0)
                    throw invalid("Este producto requiere cantidades enteras.");
                db.getCollection("productos").updateOne(session, eq("_id", id),
                        combine(inc("stock", new Decimal128(entry.cantidad())), set("actualizadoEn", now)));
                var line = new Document("productoId", id).append("nombre", product.getString("nombre"))
                        .append("unidad", product.getString("unidad"))
                        .append("cantidad", new Decimal128(entry.cantidad()))
                        .append("costoUnitario", new Decimal128(entry.costoUnitario()));
                db.getCollection("recepciones").insertOne(session, new Document("_id", receptionId)
                        .append("lineas", List.of(line)).append("por", actor).append("fecha", now));
                db.getCollection("operaciones").insertOne(session,
                        new Document("por", actor).append("tipo", "recepcion")
                                .append("clave", key.toString()).append("solicitudHash", hash)
                                .append("resultadoId", receptionId).append("fecha", now));
                return receptionId.toHexString();
            });
        }
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
