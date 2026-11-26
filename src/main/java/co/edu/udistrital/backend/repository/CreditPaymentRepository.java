package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

import com.mongodb.client.MongoClient;
import java.util.Date;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CreditPaymentRepository {
    private final MongoClient client;
    private final MongoTemplate mongo;

    public CreditPaymentRepository(MongoClient client, MongoTemplate mongo) {
        this.client = client;
        this.mongo = mongo;
    }

    public Optional<Document> findOpenCashDay(String register, String day) {
        return Optional.ofNullable(mongo.getCollection("cajasDia")
                .find(and(eq("caja", register), eq("dia", day), eq("estado", "abierta")))
                .first());
    }

    public Document savePayment(ObjectId customerId, ObjectId debtId, ObjectId cashDayId,
            ObjectId actorId, String day, String method, long amount, Date issuedAt) {
        try (var session = client.startSession()) {
            return session.withTransaction(() -> {
                Document debt = mongo.getCollection("deudasFiado")
                        .find(session, and(eq("_id", debtId), eq("clienteId", customerId), eq("estado", "pendiente")))
                        .first();
                if (debt == null) throw new PaymentException("NO_DEUDA");

                long balance = debt.getLong("saldo");
                if (balance <= 0) throw new PaymentException("NO_DEUDA");
                if (amount > balance) throw new PaymentException("EXCESO");

                Document customer = mongo.getCollection("clientesFiado").find(session, eq("_id", customerId)).first();
                if (customer == null || customer.getLong("saldo") <= 0) throw new PaymentException("NO_DEUDA");

                long remaining = balance - amount;
                String debtState = remaining == 0 ? "pagada" : "pendiente";
                mongo.getCollection("deudasFiado").updateOne(session, eq("_id", debtId),
                        combine(set("saldo", remaining), set("estado", debtState)));
                mongo.getCollection("clientesFiado").updateOne(session, eq("_id", customerId),
                        set("saldo", customer.getLong("saldo") - amount));

                ObjectId movementId = new ObjectId();
                Document movement = new Document("_id", movementId).append("cajaDiaId", cashDayId)
                        .append("dia", day).append("por", actorId).append("fecha", issuedAt)
                        .append("tipo", "abono").append("medio", method).append("monto", amount)
                        .append("clienteId", customerId).append("deudaId", debtId);
                mongo.getCollection("movimientosCaja").insertOne(session, movement);
                return movement.append("saldoRestante", remaining).append("emitidoEn", issuedAt);
            });
        }
    }

    public static class PaymentException extends RuntimeException {
        private final String code;

        public PaymentException(String code) {
            super(code);
            this.code = code;
        }

        public String code() { return code; }
    }
}
