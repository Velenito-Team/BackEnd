package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.InventoryRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {
    private final InventoryRepository repository;
    private final OperatorService operator;

    public InventoryService(InventoryRepository repository, OperatorService operator) {
        this.repository = repository;
        this.operator = operator;
    }

    public record Entry(String productoId, BigDecimal cantidad, BigDecimal costoUnitario, String proveedorId) { }

    public String receive(Entry entry, UUID key) {
        if (entry == null || key == null || entry.productoId() == null || !ObjectId.isValid(entry.productoId())
                || entry.cantidad() == null || entry.cantidad().signum() <= 0
                || entry.cantidad().stripTrailingZeros().scale() > 3 || entry.costoUnitario() == null
                || entry.costoUnitario().signum() <= 0
                || (entry.proveedorId() != null && !entry.proveedorId().isBlank()
                    && !ObjectId.isValid(entry.proveedorId())))
            throw invalid("Indica cantidad y costo positivos.");
        String hash = SaleService.sha256(entry.productoId() + ":" + entry.cantidad().stripTrailingZeros()
                + ":" + entry.costoUnitario().stripTrailingZeros());
        try {
            ObjectId providerId = entry.proveedorId() == null || entry.proveedorId().isBlank()
                    ? null : new ObjectId(entry.proveedorId());
            return repository.receive(new ObjectId(entry.productoId()), entry.cantidad(), entry.costoUnitario(),
                    providerId, operator.id(), key, hash);
        } catch (IllegalArgumentException error) {
            throw invalid(error.getMessage());
        }
    }

    private ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
