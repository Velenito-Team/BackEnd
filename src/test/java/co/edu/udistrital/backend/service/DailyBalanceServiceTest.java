package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.repository.CashClosingRepository;
import co.edu.udistrital.backend.repository.CashDayRepository;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class DailyBalanceServiceTest {
    private CashDayRepository days;
    private CashClosingRepository closings;
    private DailyBalanceService service;
    private ObjectId dayId;
    private ObjectId closingId;

    @BeforeEach
    void setUp() {
        days = mock(CashDayRepository.class);
        closings = mock(CashClosingRepository.class);
        service = new DailyBalanceService(days, closings);
        dayId = new ObjectId();
        closingId = new ObjectId();
    }

    @Test
    void calculaFlujoOperativoSeparadoDeLaDiferenciaDeCaja() {
        when(days.find("01", "2026-11-26")).thenReturn(Optional.of(
                new Document("_id", dayId).append("estado", "cerrada")
                        .append("cierreVigenteId", closingId)));
        when(closings.findById(closingId)).thenReturn(Optional.of(closing(10000, 50000, 10000, 5000, -2000)));

        var balance = service.get("2026-11-26", "01");

        assertEquals(55000, balance.flujoOperativoNeto());
        assertEquals(-2000, balance.diferenciaCaja());
    }

    @Test
    void movimientosCoincidenConElFlujoSinSumarFiadosNiBase() {
        when(days.find("01", "2026-11-26")).thenReturn(Optional.of(
                new Document("estado", "cerrada").append("cierreVigenteId", closingId)));
        when(closings.findById(closingId)).thenReturn(Optional.of(closing(30000, 20000, 0, 3000, 1000)));

        var balance = service.get("2026-11-26", "01");

        assertEquals(17000, balance.flujoOperativoNeto());
        assertEquals(20000, balance.ventasCobradas().total());
        assertEquals(3000, balance.egresos().total());
    }

    @Test
    void exigeCierrePrevio() {
        when(days.find("01", "2026-11-26")).thenReturn(Optional.of(new Document("estado", "abierta")));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.get("2026-11-26", "01"));

        assertEquals(409, error.getStatusCode().value());
    }

    private static Document closing(long base, long sales, long payments, long expenses, long difference) {
        return new Document("resumen", new Document("base", base)
                .append("ventasCobradas", totals(sales)).append("abonos", totals(payments))
                .append("egresos", totals(expenses)).append("diferencia", difference));
    }

    private static Document totals(long amount) {
        return new Document("efectivo", amount).append("nequi", 0L).append("daviplata", 0L);
    }
}
