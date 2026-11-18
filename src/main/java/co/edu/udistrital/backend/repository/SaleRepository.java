package co.edu.udistrital.backend.repository;

import co.edu.udistrital.backend.model.*;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SaleRepository extends MongoRepository<Sale, String> {
    List<Sale> findByDiaAndEstado(String dia, SaleStatus estado);

    List<Sale> findByCajaDiaIdAndEstado(String cajaDiaId, SaleStatus estado);
}
