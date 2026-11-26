package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class MonthlyBalanceRepository {
    private final MongoTemplate mongo;

    public MonthlyBalanceRepository(MongoTemplate mongo) { this.mongo = mongo; }

    public Totals totals(String month) {
        long sales = 0, payments = 0, expenses = 0;
        for (Document movement : mongo.getCollection("movimientosCaja")
                .find(regex("dia", "^" + month + "-"))) {
            long amount = movement.getLong("monto");
            switch (movement.getString("tipo")) {
                case "venta" -> sales += amount;
                case "abono" -> payments += amount;
                case "egreso" -> expenses += amount;
                default -> { }
            }
        }
        return new Totals(sales, payments, expenses);
    }

    public List<DailyTotals> closedDailyTotals(String month) {
        List<DailyTotals> result = new ArrayList<>();
        for (Document cashDay : mongo.getCollection("cajasDia")
                .find(and(regex("dia", "^" + month + "-"), eq("estado", "cerrada")))) {
            ObjectId closingId = cashDay.getObjectId("cierreVigenteId");
            if (closingId == null) continue;
            Document closing = mongo.getCollection("cierresCaja").find(eq("_id", closingId)).first();
            if (closing == null) continue;
            Document summary = closing.get("resumen", Document.class);
            result.add(new DailyTotals(cashDay.getString("dia"),
                    total(summary.get("ventasCobradas", Document.class)),
                    total(summary.get("abonos", Document.class)),
                    total(summary.get("egresos", Document.class)),
                    summary.getLong("diferencia")));
        }
        return result;
    }

    private static long total(Document values) {
        return values.getLong("efectivo") + values.getLong("nequi") + values.getLong("daviplata");
    }

    public record Totals(long ventas, long abonos, long egresos) { }
    public record DailyTotals(String dia, long ventas, long abonos, long egresos, long diferenciaCaja) { }
}
