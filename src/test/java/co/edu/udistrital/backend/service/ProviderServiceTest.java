package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.model.Provider;
import co.edu.udistrital.backend.repository.ProviderRepository;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ProviderServiceTest {
    private ProviderRepository repository;
    private OperatorService operator;
    private ProviderService service;

    @BeforeEach
    void setUp() {
        repository = mock(ProviderRepository.class);
        operator = mock(OperatorService.class);
        service = new ProviderService(repository, operator);
        when(operator.current()).thenReturn(new Document("_id", new ObjectId()).append("rol", "administrador"));
    }

    @Test
    void creaProveedorConNombreNormalizado() {
        when(repository.findByNombreNormalizado("distribuciones el sol")).thenReturn(Optional.empty());
        Provider saved = new Provider("Distribuciones El Sol", "3001234567", new ObjectId());
        saved.setId("provider-id");
        when(repository.save(any(Provider.class))).thenReturn(saved);

        var result = service.create(new ProviderService.CreateRequest("  Distribuciones  El Sól  ", "3001234567"));

        assertEquals("provider-id", result.id());
        assertEquals("Distribuciones El Sol", result.nombre());
    }

    @Test
    void rechazaNombreEquivalenteDuplicado() {
        when(repository.findByNombreNormalizado("distribuciones el sol")).thenReturn(Optional.of(new Provider()));

        var error = assertThrows(ResponseStatusException.class,
                () -> service.create(new ProviderService.CreateRequest("DISTRIBUCIONES EL SOL", null)));

        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void rechazaNombreVacio() {
        var error = assertThrows(ResponseStatusException.class,
                () -> service.create(new ProviderService.CreateRequest(" ", null)));

        assertEquals(400, error.getStatusCode().value());
    }
}
