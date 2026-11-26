package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.repository.FixedExpenseRepository;
import co.edu.udistrital.backend.repository.MonthlyBalanceRepository;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class MonthlyBalanceServiceTest {
    @Test
    void sumaBalancesDiariosYDescuentaGastosFijosUnaSolaVez() {
        var daily = mock(MonthlyBalanceRepository.class);
        var fixed = mock(FixedExpenseRepository.class);
        when(daily.closedDailyTotals("2026-11")).thenReturn(List.of(
                new MonthlyBalanceRepository.DailyTotals("2026-11-01", 1000000, 100000, 50000, 1000),
                new MonthlyBalanceRepository.DailyTotals("2026-11-02", 500000, 0, 100000, -2000)));
        when(fixed.findByMonth("2026-11")).thenReturn(List.of(
                new Document("monto", 500000L).append("conceptoNormalizado", "arriendo")));
        when(fixed.findExpectedConcepts("2026-11")).thenReturn(List.of());

        var balance = new MonthlyBalanceService(daily, fixed).monthly("2026-11");

        assertEquals(2, balance.diasCerrados());
        assertEquals(1500000, balance.ventas());
        assertEquals(100000, balance.abonos());
        assertEquals(150000, balance.egresos());
        assertEquals(500000, balance.gastosFijos());
        assertEquals(950000, balance.balance());
        assertEquals(-1000, balance.diferenciaCaja());
    }

    @Test
    void advierteConceptosFijosConfiguradosPeroNoRegistrados() {
        var daily = mock(MonthlyBalanceRepository.class);
        var fixed = mock(FixedExpenseRepository.class);
        when(daily.closedDailyTotals("2026-11")).thenReturn(List.of());
        when(fixed.findByMonth("2026-11")).thenReturn(List.of());
        when(fixed.findExpectedConcepts("2026-11")).thenReturn(List.of(
                new Document("concepto", "Internet").append("conceptoNormalizado", "internet")));

        var balance = new MonthlyBalanceService(daily, fixed).monthly("2026-11");

        assertTrue(balance.datosIncompletos());
        assertEquals(List.of("Internet"), balance.gastosFijosFaltantes());
    }

    @Test
    void exportaElBalanceMensualEnCsv() {
        var daily = mock(MonthlyBalanceRepository.class);
        var fixed = mock(FixedExpenseRepository.class);
        when(daily.closedDailyTotals("2026-11")).thenReturn(List.of());
        when(fixed.findByMonth("2026-11")).thenReturn(List.of());
        when(fixed.findExpectedConcepts("2026-11")).thenReturn(List.of());

        String csv = new MonthlyBalanceService(daily, fixed).exportCsv("2026-11");

        assertTrue(csv.startsWith("mes,diasCerrados,ventas"));
        assertTrue(csv.contains("2026-11,0,0,0,0,0"));
    }
}
