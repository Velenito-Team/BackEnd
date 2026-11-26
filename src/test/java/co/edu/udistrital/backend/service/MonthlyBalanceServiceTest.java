package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.repository.FixedExpenseRepository;
import co.edu.udistrital.backend.repository.MonthlyBalanceRepository;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Test;

class MonthlyBalanceServiceTest {
    @Test
    void incluyeGastosFijosEnElBalanceMensual() {
        var movements = mock(MonthlyBalanceRepository.class);
        var fixed = mock(FixedExpenseRepository.class);
        when(movements.totals("2026-11")).thenReturn(new MonthlyBalanceRepository.Totals(2000000L, 300000L, 400000L));
        when(fixed.findByMonth("2026-11")).thenReturn(List.of(new Document("monto", 500000L)));

        var balance = new MonthlyBalanceService(movements, fixed).monthly("2026-11");

        assertEquals(500000L, balance.gastosFijos());
        assertEquals(1400000L, balance.balance());
    }
}
