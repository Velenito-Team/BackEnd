package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.model.Provider;
import co.edu.udistrital.backend.repository.ProviderRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProviderService {
    private final ProviderRepository repository;
    private final OperatorService operator;

    public ProviderService(ProviderRepository repository, OperatorService operator) {
        this.repository = repository;
        this.operator = operator;
    }

    public record CreateRequest(String nombre, String telefono) { }
    public record ProviderView(String id, String nombre, String telefono) { }

    public ProviderView create(CreateRequest request) {
        if (request == null || request.nombre() == null || request.nombre().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del proveedor es obligatorio.");
        var currentOperator = operator.current();
        if (!"administrador".equals(currentOperator.getString("rol")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo un administrador puede registrar proveedores.");
        String name = request.nombre().strip();
        String normalized = normalize(name);
        if (repository.findByNombreNormalizado(normalized).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un proveedor con ese nombre.");
        Provider provider = new Provider(name,
                request.telefono() == null || request.telefono().isBlank() ? null : request.telefono().strip(),
                currentOperator.getObjectId("_id"));
        provider.setNombreNormalizado(normalized);
        return toView(repository.save(provider));
    }

    public List<ProviderView> list() {
        return repository.findAllByOrderByNombreNormalizadoAsc().stream().map(ProviderService::toView).toList();
    }

    static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static ProviderView toView(Provider provider) {
        return new ProviderView(provider.getId(), provider.getNombre(), provider.getTelefono());
    }
}
