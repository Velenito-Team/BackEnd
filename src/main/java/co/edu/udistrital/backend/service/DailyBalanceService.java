package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.CashClosingRepository;
import co.edu.udistrital.backend.repository.CashDayRepository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DailyBalanceService {
    private static final String DEFAULT_REGISTER = "01";
    private final CashDayRepository days;
    private final CashClosingRepository closings;

    public DailyBalanceService(CashDayRepository days, CashClosingRepository closings) {
        this.days = days;
        this.closings = closings;
    }

    public record PaymentTotals(long efectivo, long nequi, long daviplata) {
        public long total() { return efectivo + nequi + daviplata; }
    }

    public record DailyBalance(String dia, String caja, String cierreId, PaymentTotals ventasCobradas,
            PaymentTotals abonos, PaymentTotals egresos, long flujoOperativoNeto, long diferenciaCaja) { }

    public DailyBalance get(String day, String register) {
        if (day == null || day.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El día es obligatorio.");
        String cashRegister = register == null || register.isBlank() ? DEFAULT_REGISTER : register.strip();
        Document cashDay = days.find(cashRegister, day).orElseThrow(() -> notClosed(day));
        if (!"cerrada".equals(cashDay.getString("estado"))) throw notClosed(day);
        ObjectId closingId = cashDay.getObjectId("cierreVigenteId");
        if (closingId == null) throw notClosed(day);
        Document closing = closings.findById(closingId).orElseThrow(() -> notClosed(day));
        Document summary = closing.get("resumen", Document.class);
        PaymentTotals sales = totals(summary.get("ventasCobradas", Document.class));
        PaymentTotals payments = totals(summary.get("abonos", Document.class));
        PaymentTotals expenses = totals(summary.get("egresos", Document.class));
        long flow = sales.total() + payments.total() - expenses.total();
        return new DailyBalance(day, cashRegister, closingId.toHexString(), sales, payments, expenses,
                flow, summary.getLong("diferencia"));
    }

    private static PaymentTotals totals(Document values) {
        return new PaymentTotals(values.getLong("efectivo"), values.getLong("nequi"),
                values.getLong("daviplata"));
    }

    private static ResponseStatusException notClosed(String day) {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Primero debes cerrar la caja del día " + day + " para consultar el balance.");
    }
}
