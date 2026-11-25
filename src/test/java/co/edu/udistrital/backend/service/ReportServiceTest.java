package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.SalesQueryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportServiceTest {
    @Test
    void keepsCancelledSalesOutOfRevenueAndProductRanking() {
        var queries = mock(SalesQueryRepository.class);
        var day = LocalDate.of(2026, 11, 25);
        var product = new ObjectId();
        var line = new Document("productoId", product).append("nombre", "Arroz")
                .append("cantidad", new Decimal128(new BigDecimal("0.125"))).append("subtotal", 1563L);
        var paid = new Document("estado", "confirmada").append("total", 1563L).append("dia", day.toString())
                .append("metodo", "efectivo").append("lineas", List.of(line));
        var cancelled = new Document(paid).append("estado", "anulada");
        when(queries.between(day, day, null, null, null)).thenReturn(List.of(paid, cancelled));
        var result = new ReportService(queries).between(day, day, null, null, null);
        assertEquals(1563L, result.total());
        assertEquals(1L, result.ventas());
        assertEquals(1L, result.anuladas());
        assertEquals(new BigDecimal("0.125"), result.productos().getFirst().cantidad());
    }
}
