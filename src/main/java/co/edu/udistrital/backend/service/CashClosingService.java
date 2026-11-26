package co.edu.udistrital.backend.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import co.edu.udistrital.backend.repository.CashClosingRepository;
import co.edu.udistrital.backend.repository.CashDayRepository;
import co.edu.udistrital.backend.repository.CashMovementRepository;
import co.edu.udistrital.backend.repository.CreditSaleRepository;
import co.edu.udistrital.backend.repository.AuditRepository;

@Service
public class CashClosingService {
    private static final String DEFAULT_REGISTER = "01";
    private final CashClosingRepository closings;
    private final CashDayRepository days;
    private final CashMovementRepository movements;
    private final CreditSaleRepository creditSales;
    private final AuditRepository audits;
    private final OperatorService operator;

    public CashClosingService(CashClosingRepository closings, CashDayRepository days,
            CashMovementRepository movements, CreditSaleRepository creditSales, AuditRepository audits,
            OperatorService operator) {
        this.closings = closings;
        this.days = days;
        this.movements = movements;
        this.creditSales = creditSales;
        this.audits = audits;
        this.operator = operator;
    }

    public record CloseRequest(String dia, String caja, long efectivoContado) {
    }

    public record RectificationRequest(long efectivoContado, String motivo) {
    }

    public record PaymentTotals(long efectivo, long nequi, long daviplata) {
        long total() {
            return Math.addExact(Math.addExact(efectivo, nequi), daviplata);
        }
    }

    public record Summary(long base, PaymentTotals ventasCobradas, PaymentTotals abonos,
            PaymentTotals egresos, long totalVentasFiadas, long efectivoEsperado,
            long efectivoContado, long diferencia, long balanceOperativo) {
    }

    public record Closing(String id, String cajaDiaId, String dia, int revision,
            String estado, String resultado, Summary resumen, String anteriorId, String motivo) {
    }

    @Transactional
    public Closing close(CloseRequest request) {
        validateRequest(request);
        ObjectId actor = operator.id();
        String register = register(request.caja());
        Instant now = Instant.now();
        Document day = days.find(register, request.dia()).orElse(null);
        if (day != null && "cerrada".equals(day.getString("estado")))
            throw conflict("La caja del día ya está cerrada. Usa rectificación administrativa.");
        if (day == null) day = days.create(actor, register, request.dia(), Date.from(now));
        ObjectId dayId = day.getObjectId("_id");
        Summary summary = calculateSummary(dayId, request.dia(), base(day), request.efectivoContado());
        ObjectId closingId = new ObjectId();
        Document closing = closingDocument(closingId, dayId, request.dia(), 1, actor, now, summary);
        closings.save(closing);
        days.close(dayId, closingId);
        return response(closing, "cerrada");
    }

    @Transactional
    public Closing rectify(String closingId, RectificationRequest request) {
        if (!ObjectId.isValid(closingId) || request == null || request.efectivoContado() < 0
                || request.motivo() == null || request.motivo().isBlank()) {
            throw badRequest("Indica un cierre válido, efectivo contado no negativo y una justificación.");
        }
        Document currentOperator = operator.current();
        if (!"administrador".equals(currentOperator.getString("rol"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un administrador puede rectificar cierres.");
        }
        ObjectId actor = currentOperator.getObjectId("_id");
        ObjectId originalId = new ObjectId(closingId);
        Instant now = Instant.now();
        Document original = closings.findById(originalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cierre no encontrado."));
        ObjectId dayId = original.getObjectId("cajaDiaId");
        String day = original.getString("dia");
        Document dayDocument = days.findById(dayId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "La caja del día no existe."));
        Document latest = closings.findLatestByCashDayId(dayId).orElse(null);
        int revision = latest == null ? 1 : latest.getInteger("revision", 0) + 1;
        Summary summary = calculateSummary(dayId, day, base(dayDocument), request.efectivoContado());
        ObjectId newId = new ObjectId();
        Document rectification = closingDocument(newId, dayId, day, revision, actor, now, summary)
                .append("anteriorId", originalId).append("motivo", request.motivo().strip());
        closings.save(rectification);
        days.pointToClosing(dayId, newId);
        audits.save(actor, "rectificar_cierre", "cierresCaja", newId, original, rectification, request.motivo().strip());
        return response(rectification, "rectificado");
    }

    Summary calculateSummary(ObjectId dayId, String day, long base, long counted) {
        PaymentTotals sales = new PaymentTotals(0, 0, 0);
        PaymentTotals payments = new PaymentTotals(0, 0, 0);
        PaymentTotals expenses = new PaymentTotals(0, 0, 0);
        for (Document movement : movements.findByCashDayAndDay(dayId, day)) {
            long amount = movement.getLong("monto");
            String type = movement.getString("tipo");
            String method = movement.getString("medio");
            if ("reversion_venta".equals(type) || "reversion_abono".equals(type)) amount = -amount;
            if ("venta".equals(type) || "reversion_venta".equals(type)) sales = add(sales, method, amount);
            if ("abono".equals(type) || "reversion_abono".equals(type)) payments = add(payments, method, amount);
            if ("egreso".equals(type)) expenses = add(expenses, method, amount);
        }
        long creditTotal = 0;
        for (Document sale : creditSales.findConfirmedByCashDayAndDay(dayId, day)) {
            creditTotal = Math.addExact(creditTotal, sale.getLong("total"));
        }
        return summarize(base, sales, payments, expenses, creditTotal, counted);
    }

    static Summary summarize(long base, PaymentTotals sales, PaymentTotals payments,
            PaymentTotals expenses, long creditSales, long counted) {
        long expected = Math.addExact(Math.subtractExact(Math.addExact(base, sales.efectivo()), expenses.efectivo()), payments.efectivo());
        long difference = Math.subtractExact(counted, expected);
        long operating = Math.subtractExact(Math.addExact(Math.addExact(sales.total(), payments.total()), creditSales), expenses.total());
        return new Summary(base, sales, payments, expenses, creditSales, expected, counted, difference, operating);
    }

    private static PaymentTotals add(PaymentTotals totals, String method, long amount) {
        return switch (method) {
            case "efectivo" -> new PaymentTotals(Math.addExact(totals.efectivo(), amount), totals.nequi(), totals.daviplata());
            case "nequi" -> new PaymentTotals(totals.efectivo(), Math.addExact(totals.nequi(), amount), totals.daviplata());
            case "daviplata" -> new PaymentTotals(totals.efectivo(), totals.nequi(), Math.addExact(totals.daviplata(), amount));
            default -> totals;
        };
    }

    private Document closingDocument(ObjectId id, ObjectId dayId, String day, int revision, ObjectId actor,
            Instant now, Summary summary) {
        return new Document("_id", id).append("cajaDiaId", dayId).append("dia", day).append("revision", revision)
                .append("por", actor).append("fecha", Date.from(now)).append("resumen", summaryDocument(summary));
    }

    private static Document summaryDocument(Summary summary) {
        return new Document("base", summary.base()).append("ventasCobradas", totals(summary.ventasCobradas()))
                .append("abonos", totals(summary.abonos())).append("egresos", totals(summary.egresos()))
                .append("totalVentasFiadas", summary.totalVentasFiadas()).append("efectivoEsperado", summary.efectivoEsperado())
                .append("efectivoContado", summary.efectivoContado()).append("diferencia", summary.diferencia())
                .append("balanceOperativo", summary.balanceOperativo());
    }

    private static Document totals(PaymentTotals totals) {
        return new Document("efectivo", totals.efectivo()).append("nequi", totals.nequi()).append("daviplata", totals.daviplata());
    }

    private static Closing response(Document document, String status) {
        Document summary = document.get("resumen", Document.class);
        Summary value = new Summary(summary.getLong("base"), totals(summary.get("ventasCobradas", Document.class)),
                        totals(summary.get("abonos", Document.class)), totals(summary.get("egresos", Document.class)),
                        summary.getLong("totalVentasFiadas"), summary.getLong("efectivoEsperado"),
                        summary.getLong("efectivoContado"), summary.getLong("diferencia"), summary.getLong("balanceOperativo"));
        return new Closing(document.getObjectId("_id").toHexString(), document.getObjectId("cajaDiaId").toHexString(),
                document.getString("dia"), document.getInteger("revision"), status, result(value.diferencia()),
                value,
                document.getObjectId("anteriorId") == null ? null : document.getObjectId("anteriorId").toHexString(),
                document.getString("motivo"));
    }

    private static String result(long difference) {
        return difference > 0 ? "sobrante" : difference < 0 ? "faltante" : "cuadrada";
    }

    private static PaymentTotals totals(Document document) {
        return new PaymentTotals(document.getLong("efectivo"), document.getLong("nequi"), document.getLong("daviplata"));
    }

    private static long base(Document day) {
        Document opening = day.get("apertura", Document.class);
        return opening == null || opening.getLong("base") == null ? 0 : opening.getLong("base");
    }

    private static String register(String value) { return value == null || value.isBlank() ? DEFAULT_REGISTER : value.strip(); }

    private static void validateRequest(CloseRequest request) {
        if (request == null || request.dia() == null || request.efectivoContado() < 0)
            throw badRequest("Indica el día y un efectivo contado no negativo.");
        try { LocalDate.parse(request.dia()); } catch (RuntimeException error) { throw badRequest("El día debe tener formato YYYY-MM-DD."); }
    }

    private static ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
