package co.edu.udistrital.backend.controller;

import co.edu.udistrital.backend.service.CashClosingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cash-closings")
public class CashClosingController {
    private final CashClosingService service;

    public CashClosingController(CashClosingService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<CashClosingService.Closing> close(@RequestBody CashClosingService.CloseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.close(request));
    }

    @PostMapping("/{id}/rectifications")
    public CashClosingService.Closing rectify(@PathVariable String id,
            @RequestBody CashClosingService.RectificationRequest request) {
        return service.rectify(id, request);
    }
}
