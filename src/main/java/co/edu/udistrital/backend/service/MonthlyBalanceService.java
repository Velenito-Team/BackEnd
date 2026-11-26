package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.FixedExpenseRepository;
import co.edu.udistrital.backend.repository.MonthlyBalanceRepository;
import java.time.YearMonth;
import org.springframework.stereotype.Service;

@Service
public class MonthlyBalanceService {
    private final MonthlyBalanceRepository movements;
    private final FixedExpenseRepository fixedExpenses;

    public MonthlyBalanceService(MonthlyBalanceRepository movements, FixedExpenseRepository fixedExpenses) {
        this.movements = movements;
        this.fixedExpenses = fixedExpenses;
    }

    public record Balance(String mes, long ventas, long abonos, long egresos,
            long gastosFijos, long balance) { }

    public Balance monthly(String month) {
        try { YearMonth.parse(month); }
        catch (RuntimeException error) { throw new IllegalArgumentException("El mes debe tener formato AAAA-MM."); }
        var totals = movements.totals(month);
        long fixed = fixedExpenses.findByMonth(month).stream()
                .mapToLong(document -> document.getLong("monto")).sum();
        return new Balance(month, totals.ventas(), totals.abonos(), totals.egresos(), fixed,
                totals.ventas() + totals.abonos() - totals.egresos() - fixed);
    }
}
