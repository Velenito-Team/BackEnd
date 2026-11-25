package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.SalesQueryRepository;
import com.mongodb.client.MongoClient;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
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
public class SaleHistoryService {
    private final SalesQueryRepository queries;
    private final MongoClient client;
    private final MongoTemplate mongo;
    private final OperatorService operator;
    private final SaleService sales;

    public SaleHistoryService(SalesQueryRepository queries, MongoClient client, MongoTemplate mongo,
            OperatorService operator, SaleService sales) {
        this.queries = queries;
        this.client = client;
        this.mongo = mongo;
        this.operator = operator;
        this.sales = sales;
    }

    public List<SaleService.Receipt> list(LocalDate from, LocalDate to, String method, String status, String product) {
        return queries.between(from, to, method, status, product).stream().map(SaleService::receipt).toList();
    }

    public SaleService.CreatedReceipt open(String id) {
        return sales.renew(new ObjectId(id), operator.id());
    }

    public SaleService.Receipt cancel(String id, String reason) {
        if (reason == null || reason.isBlank())
            throw invalid("Indica el motivo de anulación.");
        var actor = operator.id();
        var saleId = new ObjectId(id);
        var db = mongo.getDb();
        try (var session = client.startSession()) {
            return session.withTransaction(() -> {
                var sale = db.getCollection("ventas").find(session, eq("_id", saleId)).first();
                if (sale == null)
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada.");
                if ("anulada".equals(sale.getString("estado")))
                    return SaleService.receipt(sale);
                if (!LocalDate.now(ZoneId.of("America/Bogota")).toString().equals(sale.getString("dia")))
                    throw invalid("Solo se pueden anular ventas del día, antes del cierre.");
                var day = db.getCollection("cajasDia").updateOne(session,
                        and(eq("_id", sale.getObjectId("cajaDiaId")), eq("estado", "abierta")), inc("version", 1));
                if (day.getModifiedCount() != 1)
                    throw invalid("La caja ya está cerrada.");
                if ("fiado".equals(sale.getString("metodo"))) {
                    var debt = db.getCollection("deudasFiado").find(session, eq("ventaId", saleId)).first();
                    if (debt == null || !debt.get("saldo").equals(debt.get("importeOriginal")))
                        throw invalid("La venta fiada requiere conciliar sus abonos antes de anular.");
                    db.getCollection("deudasFiado").updateOne(session, eq("_id", debt.getObjectId("_id")),
                            combine(set("saldo", 0L), set("estado", "anulada")));
                    db.getCollection("clientesFiado").updateOne(session, eq("_id", sale.getObjectId("clienteId")),
                            inc("saldo", -debt.getLong("saldo")));
                }
                for (var line : sale.getList("lineas", Document.class)) {
                    var result = db.getCollection("productos").updateOne(session,
                            and(eq("_id", line.getObjectId("productoId")), eq("unidad", line.getString("unidad"))),
                            combine(inc("stock", line.get("cantidad", Decimal128.class)),
                                    set("actualizadoEn", new Date())));
                    if (result.getMatchedCount() != 1)
                        throw invalid("Revisa la unidad actual del producto antes de restituir stock.");
                }
                var movement = db.getCollection("movimientosCaja")
                        .find(session, and(eq("ventaId", saleId), eq("tipo", "venta"))).first();
                if (movement != null) {
                    db.getCollection("movimientosCaja").insertOne(session,
                            new Document("cajaDiaId", sale.getObjectId("cajaDiaId"))
                                    .append("dia", sale.getString("dia")).append("por", actor)
                                    .append("fecha", new Date())
                                    .append("tipo", "reversion_venta").append("medio", sale.getString("metodo"))
                                    .append("monto", movement.get("monto")).append("ventaId", saleId)
                                    .append("origenId", movement.getObjectId("_id"))
                                    .append("descripcion", reason.strip()));
                }
                var cancellation = new Document("por", actor).append("fecha", new Date()).append("motivo",
                        reason.strip());
                db.getCollection("ventas").updateOne(session, eq("_id", saleId),
                        combine(set("estado", "anulada"), set("anulacion", cancellation)));
                db.getCollection("auditoria").insertOne(session, new Document("por", actor).append("fecha", new Date())
                        .append("accion", "anular_venta").append("entidad", "ventas").append("entidadId", saleId)
                        .append("anterior", new Document("estado", "confirmada"))
                        .append("nuevo", new Document("estado", "anulada")).append("motivo", reason.strip()));
                sale.append("estado", "anulada");
                return SaleService.receipt(sale);
            });
        }
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.CONFLICT, message);
    }
}
