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
    private final InventoryService inventory;
    private final OperatorService operator;
    private final MongoTemplate mongo;

    public WorkspaceController(SaleHistoryService history, ReportService reports, InventoryService inventory,
            OperatorService operator, MongoTemplate mongo) {
        this.history = history;
        this.reports = reports;
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
