package co.edu.udistrital.backend.controller;

import co.edu.udistrital.backend.service.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WorkspaceController {
    private final SaleHistoryService history;
    private final ReportService reports;
    private final FixedExpenseService fixedExpenses;
    private final MonthlyBalanceService balances;
    private final InventoryService inventory;
    private final OperatorService operator;
    private final MongoTemplate mongo;

    public WorkspaceController(SaleHistoryService history, ReportService reports, FixedExpenseService fixedExpenses,
            MonthlyBalanceService balances, InventoryService inventory, OperatorService operator, MongoTemplate mongo) {
        this.history = history;
        this.reports = reports;
        this.fixedExpenses = fixedExpenses;
        this.balances = balances;
        this.inventory = inventory;
        this.operator = operator;
        this.mongo = mongo;
    }

    @GetMapping("/sales")
    public List<SaleService.Receipt> sales(@RequestParam LocalDate desde, @RequestParam LocalDate hasta,
            @RequestParam(required = false) String metodo, @RequestParam(required = false) String estado,
            @RequestParam(required = false) String productoId) {
        return history.list(desde, hasta, metodo, estado, productoId);
    }

    @GetMapping("/reports")
    public ReportService.Report report(@RequestParam LocalDate desde, @RequestParam LocalDate hasta,
            @RequestParam(required = false) String metodo, @RequestParam(required = false) String estado,
            @RequestParam(required = false) String productoId) {
        return reports.between(desde, hasta, metodo, estado, productoId);
    }

    @GetMapping("/fixed-expenses/summary")
    public FixedExpenseService.MonthlySummary fixedExpenseSummary(@RequestParam String mes) {
        return fixedExpenses.summary(mes);
    }

    @GetMapping("/balance/monthly")
    public MonthlyBalanceService.Balance monthlyBalance(@RequestParam String mes) {
        return balances.monthly(mes);
    }

    @GetMapping(value = "/balance/monthly/export", produces = "text/csv")
    public org.springframework.http.ResponseEntity<String> exportMonthlyBalance(@RequestParam String mes) {
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=balance-" + mes + ".csv")
                .body(balances.exportCsv(mes));
    }

    @PostMapping("/sales/{id}/receipt")
    public SaleService.CreatedReceipt receipt(@PathVariable String id) {
        return history.open(id);
    }

    public record Cancellation(String motivo) {
    }

    @PostMapping("/sales/{id}/cancel")
    public SaleService.Receipt cancel(@PathVariable String id, @RequestBody Cancellation request) {
        return history.cancel(id, request.motivo());
    }

    @PostMapping("/inventory/receipts")
    public Map<String, String> receive(@RequestBody InventoryService.Entry entry,
            @RequestHeader("Idempotency-Key") UUID key) {
        return Map.of("id", inventory.receive(entry, key));
    }

    @GetMapping("/settings")
    public Map<String, Object> settings() {
        var user = operator.current();
        var hello = mongo.executeCommand(new org.bson.Document("hello", 1));
        return Map.of("tienda", "Tienda El Veleñito", "caja", "01",
                "operador", user.getString("nombre"), "usuario", user.getString("usuarioNormalizado"),
                "rol", user.getString("rol"), "replicaSet", hello.getString("setName"),
                "primario", hello.getBoolean("isWritablePrimary", false));
    }
}
