package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.repository.FixedExpenseRepository;
import co.edu.udistrital.backend.repository.MonthlyBalanceRepository;
import java.time.YearMonth;
import java.util.List;
import org.bson.Document;
import org.springframework.stereotype.Service;

@Service
public class MonthlyBalanceService {
    private final MonthlyBalanceRepository dailyBalances;
    private final FixedExpenseRepository fixedExpenses;

    public MonthlyBalanceService(MonthlyBalanceRepository dailyBalances, FixedExpenseRepository fixedExpenses) {
        this.dailyBalances = dailyBalances;
        this.fixedExpenses = fixedExpenses;
    }

    public record Balance(String mes, int diasCerrados, long ventas, long abonos, long egresos,
            long gastosFijos, long diferenciaCaja, long balance, boolean datosIncompletos,
            List<String> gastosFijosFaltantes) { }

    public Balance monthly(String month) {
        validateMonth(month);
        var daily = dailyBalances.closedDailyTotals(month);
        long sales = daily.stream().mapToLong(MonthlyBalanceRepository.DailyTotals::ventas).sum();
        long payments = daily.stream().mapToLong(MonthlyBalanceRepository.DailyTotals::abonos).sum();
        long expenses = daily.stream().mapToLong(MonthlyBalanceRepository.DailyTotals::egresos).sum();
        long cashDifference = daily.stream().mapToLong(MonthlyBalanceRepository.DailyTotals::diferenciaCaja).sum();
        List<Document> registered = fixedExpenses.findByMonth(month);
        long fixed = registered.stream().mapToLong(document -> document.getLong("monto")).sum();
        List<String> registeredNames = registered.stream()
                .map(document -> document.getString("conceptoNormalizado")).toList();
        List<String> missing = fixedExpenses.findExpectedConcepts(month).stream()
                .filter(document -> !registeredNames.contains(document.getString("conceptoNormalizado")))
                .map(document -> document.getString("concepto")).toList();
        return new Balance(month, daily.size(), sales, payments, expenses, fixed, cashDifference,
                sales + payments - expenses - fixed, !missing.isEmpty(), missing);
    }

    public String exportCsv(String month) {
        Balance balance = monthly(month);
        return "mes,diasCerrados,ventas,abonos,egresos,gastosFijos,diferenciaCaja,balance,datosIncompletos,gastosFijosFaltantes\n"
                + balance.mes() + "," + balance.diasCerrados() + "," + balance.ventas() + ","
                + balance.abonos() + "," + balance.egresos() + "," + balance.gastosFijos() + ","
                + balance.diferenciaCaja() + "," + balance.balance() + "," + balance.datosIncompletos()
                + ",\"" + String.join(" | ", balance.gastosFijosFaltantes()) + "\"\n";
    }

    private static void validateMonth(String month) {
        try { YearMonth.parse(month); }
        catch (RuntimeException error) { throw new IllegalArgumentException("El mes debe tener formato AAAA-MM."); }
    }
}
