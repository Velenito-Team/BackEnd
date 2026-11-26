package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.CreditRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreditService {
    private final CreditRepository repository;
    private final OperatorService operator;

    public CreditService(CreditRepository repository, OperatorService operator) {
        this.repository = repository;
        this.operator = operator;
    }

    public record Customer(String id, String nombre, String telefono, long saldo) { }
    public record CustomerRequest(String nombre, String telefono, boolean autorizacion, String evidencia) { }
    public record Credit(String id, Instant fecha, long total, long saldo, String estado,
            List<SaleService.ReceiptLine> lineas) { }

    public List<Customer> customers() {
        return repository.findAuthorizedCustomers().stream()
                .map(d -> new Customer(d.getObjectId("_id").toHexString(), d.getString("nombre"),
                        d.getString("telefono"), d.getLong("saldo"))).toList();
    }

    public List<Credit> credits(String customerId) {
        if (!ObjectId.isValid(customerId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente no es válido.");

        List<Document> debts = repository.findDebtsByCustomer(new ObjectId(customerId));
        List<ObjectId> saleIds = debts.stream().map(debt -> debt.getObjectId("ventaId")).toList();
        var salesById = new HashMap<ObjectId, Document>();
        repository.findSalesByIds(saleIds).forEach(sale -> salesById.put(sale.getObjectId("_id"), sale));

        return debts.stream().map(debt -> {
            Document sale = salesById.get(debt.getObjectId("ventaId"));
            if (sale == null)
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "No se encontró la venta asociada a una deuda del cliente.");
            List<SaleService.ReceiptLine> lines = sale.getList("lineas", Document.class).stream()
                    .map(CreditService::receiptLine).toList();
            return new Credit(debt.getObjectId("_id").toHexString(), sale.getDate("fecha").toInstant(),
                    sale.getLong("total"), debt.getLong("saldo"), sale.getString("estado"), lines);
        }).toList();
    }

    private static SaleService.ReceiptLine receiptLine(Document line) {
        return new SaleService.ReceiptLine(line.getObjectId("productoId").toHexString(), line.getString("nombre"),
                line.getString("unidad"), decimal(line, "cantidad"), decimal(line, "precioUnitario"),
                line.getLong("subtotal"));
    }

    private static BigDecimal decimal(Document document, String field) {
        return document.get(field, Decimal128.class).bigDecimalValue();
    }

    public Customer create(CustomerRequest request) {
        if (request == null || request.nombre() == null || request.nombre().isBlank() || !request.autorizacion()
                || request.evidencia() == null || request.evidencia().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registra nombre y constancia de autorizacion previa del cliente.");
        ObjectId actor = operator.id();
        ObjectId id = new ObjectId();
        ObjectId authorization = new ObjectId();
        Date now = new Date();
        String name = request.nombre().strip();
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFC).replaceAll("(?U)\\s+", " ")
                .toLowerCase(Locale.ROOT);
        Document customer = new Document("_id", id).append("nombre", name)
                .append("nombreNormalizado", normalized).append("saldo", 0L)
                .append("autorizacionVigenteId", authorization).append("por", actor).append("fecha", now);
        if (request.telefono() != null && !request.telefono().isBlank())
            customer.append("telefono", request.telefono().strip());
        Document authorizationDocument = new Document("_id", authorization).append("clienteId", id)
                .append("base", "consentimiento").append("decision", "concedida")
                .append("finalidad", "Gestion de ventas fiadas, abonos y consulta de saldos")
                .append("versionAviso", "fiados-v1").append("evidenciaRef", request.evidencia().strip())
                .append("por", actor).append("fecha", now);
        repository.saveCustomerWithAuthorization(customer, authorizationDocument);
        return new Customer(id.toHexString(), name, customer.getString("telefono"), 0);
    }
}
