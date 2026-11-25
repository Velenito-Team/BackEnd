package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.SalesQueryRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.springframework.stereotype.Service;

@Service
public class ReportService {
    private final SalesQueryRepository sales;

    public ReportService(SalesQueryRepository sales) {
        this.sales = sales;
    }

    public record ProductTotal(String productoId, String nombre, BigDecimal cantidad, long total) {
    }

    public record Report(long ventas, long anuladas, long total, BigDecimal promedio,
            Map<String, Long> porMedio, Map<String, Long> porDia, List<ProductTotal> productos) {
    }

    public Report between(LocalDate from, LocalDate to, String method, String status, String product) {
        var records = sales.between(from, to, method, status, product);
        var byMethod = new LinkedHashMap<String, Long>();
        var byDay = new java.util.TreeMap<String, Long>();
        var byProduct = new LinkedHashMap<String, ProductTotal>();
        long count = 0, cancelled = 0, total = 0;
        for (var sale : records) {
            if ("anulada".equals(sale.getString("estado"))) {
                cancelled++;
                continue;
            }
            count++;
            long value = ((Number) sale.get("total")).longValue();
            total = Math.addExact(total, value);
            byMethod.merge(sale.getString("metodo"), value, Math::addExact);
            byDay.merge(sale.getString("dia"), value, Math::addExact);
            for (var line : sale.getList("lineas", Document.class)) {
                String id = line.getObjectId("productoId").toHexString();
                var old = byProduct.getOrDefault(id,
                        new ProductTotal(id, line.getString("nombre"), BigDecimal.ZERO, 0));
                byProduct.put(id, new ProductTotal(id, line.getString("nombre"),
                        old.cantidad().add(line.get("cantidad", Decimal128.class).bigDecimalValue()),
                        Math.addExact(old.total(), ((Number) line.get("subtotal")).longValue())));
            }
        }
        var ranked = byProduct.values().stream()
                .sorted(java.util.Comparator.comparing(ProductTotal::cantidad).reversed()).toList();
        return new Report(count, cancelled, total,
                count == 0 ? BigDecimal.ZERO
                        : BigDecimal.valueOf(total).divide(BigDecimal.valueOf(count), 0, RoundingMode.HALF_UP),
                byMethod, byDay, ranked);
    }
}
