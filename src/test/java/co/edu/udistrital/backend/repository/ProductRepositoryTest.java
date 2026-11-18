package co.edu.udistrital.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import co.edu.udistrital.backend.TestcontainersConfiguration;
import co.edu.udistrital.backend.model.Product;
import co.edu.udistrital.backend.model.Unit;

@DataMongoTest
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {
    @Autowired private ProductRepository repository;

    @BeforeEach void clean() { repository.deleteAll(); }

    @Test
    void searchesActiveProductsByNormalizedName() {
        Product product = new Product("Arroz Diana", "Granos", BigDecimal.ONE, BigDecimal.TEN, Unit.kg);
        repository.save(product);
        assertThat(repository.findByActivoTrueAndNombreNormalizadoContaining("arroz")).hasSize(1).first().extracting(Product::getNombre).isEqualTo("Arroz Diana");
    }

    @Test
    void findsProductsAtOrBelowMinimumStock() {
        Product product = new Product("Pan", "Panaderia", BigDecimal.ONE, BigDecimal.TEN, Unit.unidad);
        product.setStock(BigDecimal.valueOf(2)); product.setUmbralMinimo(BigDecimal.valueOf(3)); repository.save(product);
        assertThat(repository.findByActivoTrueAndStockLessThanEqual(BigDecimal.valueOf(2))).hasSize(1);
    }
}
