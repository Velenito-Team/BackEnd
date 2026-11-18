package co.edu.udistrital.backend.repository;

import co.edu.udistrital.backend.model.Product;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByActivoTrueAndNombreNormalizadoContaining(String nombre);

    List<Product> findByActivoTrueAndStockLessThanEqual(java.math.BigDecimal stock);
}
