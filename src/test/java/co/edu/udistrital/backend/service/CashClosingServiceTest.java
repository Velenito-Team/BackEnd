package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import co.edu.udistrital.backend.service.CashClosingService.PaymentTotals;
import co.edu.udistrital.backend.service.CashClosingService.Summary;
import co.edu.udistrital.backend.repository.AuditRepository;
import co.edu.udistrital.backend.repository.CashClosingRepository;
import co.edu.udistrital.backend.repository.CashDayRepository;
import co.edu.udistrital.backend.repository.CashMovementRepository;
import co.edu.udistrital.backend.repository.CreditSaleRepository;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CashClosingServiceTest {
    @Mock CashClosingRepository closings;
    @Mock CashDayRepository days;
    @Mock CashMovementRepository movements;
    @Mock CreditSaleRepository creditSales;
    @Mock AuditRepository audits;
    @Mock OperatorService operator;

    @Test
    void calculatesShortageWhenCountedCashIsLowerThanExpected() {
        Summary summary = CashClosingService.summarize(10000, new PaymentTotals(50000, 10000, 0),
                new PaymentTotals(0, 0, 0), new PaymentTotals(5000, 0, 0), 0,
                53000);
        assertEquals(-2000, summary.diferencia());
        assertEquals("faltante", result(summary.diferencia()));
    }

    @Test
    void calculatesSurplusOnAdayWithoutSalesUsingOpeningBase() {
        Summary summary = CashClosingService.summarize(30000, new PaymentTotals(0, 0, 0),
                new PaymentTotals(0, 0, 0), new PaymentTotals(0, 0, 0), 0,
                32000);
        assertEquals(2000, summary.diferencia());
        assertEquals(30000, summary.efectivoEsperado());
        assertEquals("sobrante", result(summary.diferencia()));
    }

    @Test
    void rectificationKeepsTraceabilityFields() {
        var request = new CashClosingService.RectificationRequest(42000, "Se contó nuevamente el efectivo");
        assertEquals("Se contó nuevamente el efectivo", request.motivo());
        assertEquals(42000, request.efectivoContado());
    }

    @Test
    void closesAnOpenDayThroughRepositories() {
        ObjectId actor = new ObjectId();
        ObjectId cashDay = new ObjectId();
        Document day = new Document("_id", cashDay).append("estado", "abierta")
                .append("apertura", new Document("base", 10000L));
        when(operator.id()).thenReturn(actor);
        when(days.find("01", "2026-11-26")).thenReturn(Optional.of(day));
        when(movements.findByCashDayAndDay(cashDay, "2026-11-26")).thenReturn(java.util.List.of());
        when(creditSales.findConfirmedByCashDayAndDay(cashDay, "2026-11-26")).thenReturn(java.util.List.of());

        CashClosingService service = new CashClosingService(closings, days, movements, creditSales, audits, operator);
        var result = service.close(new CashClosingService.CloseRequest("2026-11-26", "01", 10000));

        assertEquals("cuadrada", result.resultado());
        verify(closings).save(any(Document.class));
        verify(days).close(any(ObjectId.class), any(ObjectId.class));
    }

    @Test
    void opensDayWithNonNegativeBase() {
        ObjectId actor = new ObjectId();
        ObjectId cashDay = new ObjectId();
        when(operator.current()).thenReturn(new Document("_id", actor).append("rol", "administrador"));
        when(days.find("01", "2026-11-27")).thenReturn(Optional.empty());
        when(days.create(eq(actor), eq("01"), eq("2026-11-27"), eq(25000L), any(java.util.Date.class)))
                .thenReturn(new Document("_id", cashDay));

        CashClosingService service = new CashClosingService(closings, days, movements, creditSales, audits, operator);
        var result = service.open(new CashClosingService.OpenRequest("2026-11-27", "01", 25000));

        assertEquals(25000, result.base());
        verify(days).create(eq(actor), eq("01"), eq("2026-11-27"), eq(25000L), any(java.util.Date.class));
    }

    @Test
    void rejectsDuplicateOpening() {
        when(operator.current()).thenReturn(new Document("_id", new ObjectId()).append("rol", "administrador"));
        when(days.find("01", "2026-11-27")).thenReturn(Optional.of(new Document("_id", new ObjectId())));

        CashClosingService service = new CashClosingService(closings, days, movements, creditSales, audits, operator);
        var error = org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> service.open(new CashClosingService.OpenRequest("2026-11-27", "01", 0)));

        assertEquals(409, error.getStatusCode().value());
    }

    @Test
    void closesWithoutOpeningUsingZeroBase() {
        ObjectId actor = new ObjectId();
        ObjectId cashDay = new ObjectId();
        Document created = new Document("_id", cashDay).append("estado", "abierta");
        when(operator.id()).thenReturn(actor);
        when(days.find("01", "2026-11-28")).thenReturn(Optional.empty());
        when(days.create(eq(actor), eq("01"), eq("2026-11-28"), any(java.util.Date.class))).thenReturn(created);
        when(movements.findByCashDayAndDay(cashDay, "2026-11-28")).thenReturn(java.util.List.of());
        when(creditSales.findConfirmedByCashDayAndDay(cashDay, "2026-11-28")).thenReturn(java.util.List.of());

        CashClosingService service = new CashClosingService(closings, days, movements, creditSales, audits, operator);
        var result = service.close(new CashClosingService.CloseRequest("2026-11-28", "01", 0));

        assertEquals(0, result.resumen().base());
    }

    private static String result(long difference) {
        return difference > 0 ? "sobrante" : difference < 0 ? "faltante" : "cuadrada";
    }
}
