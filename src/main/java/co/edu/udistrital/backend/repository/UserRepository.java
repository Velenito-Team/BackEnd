package co.edu.udistrital.backend.repository;

import co.edu.udistrital.backend.model.User;
import java.util.Optional;
import java.util.List;
import co.edu.udistrital.backend.model.Role;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsuarioNormalizado(String usuarioNormalizado);
    List<User> findAllByOrderByUsuarioNormalizadoAsc();
    long countByRolAndActivoTrue(Role rol);
}
