package co.edu.udistrital.backend.repository;

import co.edu.udistrital.backend.model.User;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByUsuarioNormalizado(String usuarioNormalizado);
}
