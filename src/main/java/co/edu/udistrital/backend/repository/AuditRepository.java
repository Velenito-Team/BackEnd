package co.edu.udistrital.backend.repository;

import java.util.Date;
import org.bson.Document;
import org.bson.types.ObjectId;
import co.edu.udistrital.backend.model.Product;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuditRepository {
    private final MongoTemplate mongo;

    public AuditRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public void save(ObjectId actor, String action, String entity, ObjectId entityId,
            Document before, Document after, String reason) {
        mongo.getCollection("auditoria").insertOne(new Document("por", actor).append("fecha", new Date())
                .append("accion", action).append("entidad", entity).append("entidadId", entityId)
                .append("anterior", before).append("nuevo", after).append("motivo", reason));
    }

    public void saveProduct(ObjectId actor, String action, Product saved, Product before) {
        Document after = new Document();
        mongo.getConverter().write(saved, after);
        Document previous = null;
        if (before != null) {
            previous = new Document();
            mongo.getConverter().write(before, previous);
        } else if ("desactivar_producto".equals(action)) {
            previous = new Document("activo", true);
        }
        save(actor, action, "productos", new ObjectId(saved.getId()), previous, after, null);
    }
}
