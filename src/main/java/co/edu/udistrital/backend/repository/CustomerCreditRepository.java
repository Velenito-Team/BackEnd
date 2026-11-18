package co.edu.udistrital.backend.repository;

import co.edu.udistrital.backend.model.CustomerCredit;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerCreditRepository extends MongoRepository<CustomerCredit, String> {
    List<CustomerCredit> findBySaldoGreaterThan(BigDecimal saldo);

    List<CustomerCredit> findByNombreNormalizadoContaining(String nombre);
}
