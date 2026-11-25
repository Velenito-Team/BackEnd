package co.edu.udistrital.backend.controller;

import co.edu.udistrital.backend.service.SaleService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final SaleService service;

    public InvoiceController(SaleService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<SaleService.CreatedReceipt> create(@RequestBody SaleService.InvoiceRequest invoice,
            @RequestHeader("Idempotency-Key") UUID key) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(service.saveInvoice(invoice, key));
    }

    @GetMapping("/receipt/{token}")
    public ResponseEntity<SaleService.Receipt> readReceipt(@PathVariable String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow")
                .body(service.readReceipt(token));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleInvoiceError(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(Map.of("message",
                        error.getReason() == null ? "No se pudo procesar el comprobante." : error.getReason()));
    }
}
