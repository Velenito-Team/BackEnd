package co.edu.udistrital.backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class SaleTest {
    @Test
    void recalculatesTotalFromLines() {
        Sale sale = new Sale();
        sale.setLineas(List.of(
            new SaleLine("p1", "Pan", Unit.unidad, BigDecimal.valueOf(2), BigDecimal.valueOf(1_500)),
            new SaleLine("p2", "Leche", Unit.l, BigDecimal.valueOf(1), BigDecimal.valueOf(4_000))));
        sale.recalculateTotal();
        assertEquals(BigDecimal.valueOf(7_000), sale.getTotal());
    }
}
