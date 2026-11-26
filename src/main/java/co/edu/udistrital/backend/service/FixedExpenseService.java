package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.FixedExpenseRepository;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FixedExpenseService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");
    private final FixedExpenseRepository repository;
    private final OperatorService operator;
    private final Clock clock;

    @Autowired
    public FixedExpenseService(FixedExpenseRepository repository, OperatorService operator) {
        this(repository, operator, Clock.system(BUSINESS_ZONE));
    }

    FixedExpenseService(FixedExpenseRepository repository, OperatorService operator, Clock clock) {
        this.repository = repository;
        this.operator = operator;
        this.clock = clock;
    }

    public record FixedExpenseRequest(String concepto, long monto, String mes, int diaRecordatorio) { }
    public record FixedExpense(String id, String concepto, long monto, String mes, int diaRecordatorio) { }
    public record Reminder(String id, String concepto, int diaRecordatorio, boolean pendiente) { }
    public record MonthlySummary(String mes, long total, List<FixedExpense> gastos,
            List<Reminder> recordatorios) { }

    public FixedExpense save(FixedExpenseRequest request) {
        validate(request);
        Document currentOperator = operator.current();
        if (!"administrador".equals(currentOperator.getString("rol")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permisos contables para registrar gastos fijos.");
        String concept = request.concepto().strip();
        String normalized = Normalizer.normalize(concept, Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT);
        Document saved = repository.save(concept, normalized, request.mes(), request.monto(),
                request.diaRecordatorio(), currentOperator.getObjectId("_id"),
                java.util.Date.from(clock.instant()));
        return toResponse(saved);
    }

    public MonthlySummary summary(String month) {
        validateMonth(month);
        List<Document> saved = repository.findByMonth(month);
        LocalDate today = LocalDate.now(clock);
        List<FixedExpense> expenses = saved.stream().map(FixedExpenseService::toResponse).toList();
        long total = expenses.stream().mapToLong(FixedExpense::monto).sum();
        List<Reminder> reminders = saved.stream().map(document -> {
            int reminderDay = document.getInteger("diaRecordatorio");
            boolean sameMonth = today.toString().startsWith(month);
            boolean dueSoon = sameMonth && today.getDayOfMonth() <= reminderDay
                    && reminderDay - today.getDayOfMonth() <= 3;
            return new Reminder(document.getObjectId("_id").toHexString(),
                    document.getString("concepto"), reminderDay, dueSoon);
        }).toList();
        return new MonthlySummary(month, total, expenses, reminders);
    }

    private static FixedExpense toResponse(Document document) {
        return new FixedExpense(document.getObjectId("_id").toHexString(), document.getString("concepto"),
                document.getLong("monto"), document.getString("mes"), document.getInteger("diaRecordatorio"));
    }

    private static void validate(FixedExpenseRequest request) {
        if (request == null || request.concepto() == null || request.concepto().isBlank())
            throw badRequest("El tipo de gasto es obligatorio.");
        validateMonth(request.mes());
        if (request.monto() <= 0) throw badRequest("El monto debe ser mayor que cero.");
        if (request.diaRecordatorio() < 1 || request.diaRecordatorio() > 28)
            throw badRequest("El día de recordatorio debe estar entre 1 y 28.");
    }

    private static void validateMonth(String month) {
        try { YearMonth.parse(month); }
        catch (RuntimeException error) { throw badRequest("El mes debe tener formato AAAA-MM."); }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
