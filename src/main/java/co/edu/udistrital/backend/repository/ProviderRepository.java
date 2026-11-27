package co.edu.udistrital.backend.repository;

import co.edu.udistrital.backend.model.Provider;
import java.util.Optional;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProviderRepository extends MongoRepository<Provider, String> {
    Optional<Provider> findByNombreNormalizado(String nombreNormalizado);
    List<Provider> findAllByOrderByNombreNormalizadoAsc();
}
