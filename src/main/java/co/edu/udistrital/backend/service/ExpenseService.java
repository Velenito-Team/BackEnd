package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.ExpenseRepository;
import java.time.LocalDate;
import java.util.Date;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExpenseService {
    private static final String REGISTER = "01";
    private final ExpenseRepository repository;
    private final OperatorService operator;

    public ExpenseService(ExpenseRepository repository, OperatorService operator) {
        this.repository = repository;
        this.operator = operator;
    }

    public record ExpenseRequest(String dia, String medio, long monto, String descripcion, String proveedorId) { }

    public record Expense(String id, String cajaDiaId, String dia, String medio, long monto,
            String descripcion, String proveedorId) { }

    public Expense save(ExpenseRequest request) {
        validate(request);
        Document currentOperator = operator.current();
        if (!"administrador".equals(currentOperator.getString("rol"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permisos contables para registrar gastos.");
        }
        ObjectId providerId = request.proveedorId() == null || request.proveedorId().isBlank()
                ? null : new ObjectId(request.proveedorId());
        ObjectId actor = currentOperator.getObjectId("_id");
        Document cashDay = repository.findOpenCashDay(REGISTER, request.dia())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No existe una caja abierta para el día indicado."));
        Document saved = repository.save(cashDay.getObjectId("_id"), request.dia(), actor,
                request.medio(), request.monto(), request.descripcion().strip(), providerId, new Date());
        return toResponse(saved);
    }

    private static void validate(ExpenseRequest request) {
        if (request == null || request.dia() == null) throw badRequest("El día es obligatorio.");
        try { LocalDate.parse(request.dia()); } catch (RuntimeException error) { throw badRequest("El día no es válido."); }
        if (request.monto() <= 0) throw badRequest("El monto debe ser mayor que cero.");
        if (request.descripcion() == null || request.descripcion().isBlank())
            throw badRequest("La descripción del gasto es obligatoria.");
        if (request.medio() == null || !request.medio().matches("efectivo|nequi|daviplata"))
            throw badRequest("El medio debe ser efectivo, nequi o daviplata.");
        if (request.proveedorId() != null && !request.proveedorId().isBlank()
                && !ObjectId.isValid(request.proveedorId())) throw badRequest("El proveedor no es válido.");
    }

    private static Expense toResponse(Document document) {
        ObjectId provider = document.getObjectId("proveedorId");
        return new Expense(document.getObjectId("_id").toHexString(), document.getObjectId("cajaDiaId").toHexString(),
                document.getString("dia"), document.getString("medio"), document.getLong("monto"),
                document.getString("descripcion"), provider == null ? null : provider.toHexString());
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
