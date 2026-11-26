package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.regex;

import org.bson.Document;
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

    public record Totals(long ventas, long abonos, long egresos) { }
}
