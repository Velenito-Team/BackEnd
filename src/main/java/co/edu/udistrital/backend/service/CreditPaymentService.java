package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.CreditPaymentRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CreditPaymentService {
    private static final String REGISTER = "01";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private final CreditPaymentRepository repository;
    private final OperatorService operator;

    public CreditPaymentService(CreditPaymentRepository repository, OperatorService operator) {
        this.repository = repository;
        this.operator = operator;
    }

    public record PaymentRequest(String deudaId, long monto, String medio) { }
    public record PaymentReceipt(String comprobanteId, String clienteId, String deudaId,
            long monto, String medio, long saldoRestante, Instant emitidoEn) { }

    public PaymentReceipt pay(String customerId, PaymentRequest request) {
        validate(customerId, request);
        ObjectId customerObjectId = new ObjectId(customerId);
        ObjectId debtObjectId = new ObjectId(request.deudaId());
        ObjectId actorId = operator.id();
        String day = LocalDate.now(BUSINESS_ZONE).toString();
        Document cashDay = repository.findOpenCashDay(REGISTER, day)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No existe una caja abierta para el día actual."));
        try {
            Document saved = repository.savePayment(customerObjectId, debtObjectId,
                    cashDay.getObjectId("_id"), actorId, day, request.medio(), request.monto(), new Date());
            return new PaymentReceipt(saved.getObjectId("_id").toHexString(), customerId,
                    request.deudaId(), saved.getLong("monto"), saved.getString("medio"),
                    saved.getLong("saldoRestante"), saved.getDate("emitidoEn").toInstant());
        } catch (CreditPaymentRepository.PaymentException error) {
            if ("EXCESO".equals(error.code()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El abono no puede superar el saldo pendiente.");
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El cliente no tiene una deuda pendiente para abonar.");
        }
    }

    private static void validate(String customerId, PaymentRequest request) {
        if (!ObjectId.isValid(customerId)) throw badRequest("El cliente no es válido.");
        if (request == null || !ObjectId.isValid(request.deudaId())) throw badRequest("La deuda no es válida.");
        if (request.monto() <= 0) throw badRequest("El monto debe ser mayor que cero.");
        if (request.medio() == null || !request.medio().matches("efectivo|nequi|daviplata"))
            throw badRequest("El medio debe ser efectivo, nequi o daviplata.");
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
