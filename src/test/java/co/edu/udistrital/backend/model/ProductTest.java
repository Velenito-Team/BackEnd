package co.edu.udistrital.backend.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductTest {
    @Test
    void normalizesSearchFieldsWhenCreated() {
        Product product = new Product("  Arroz  ", "  Granos ", BigDecimal.TEN, BigDecimal.valueOf(14_000), Unit.kg);
        assertEquals("arroz", product.getNombreNormalizado());
        assertEquals("granos", product.getCategoriaNormalizada());
        assertEquals(BigDecimal.ZERO, product.getStock());
    }
}
