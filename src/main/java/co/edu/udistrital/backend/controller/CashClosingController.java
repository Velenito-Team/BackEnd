package co.edu.udistrital.backend.controller;

import co.edu.udistrital.backend.service.CashClosingService;
import co.edu.udistrital.backend.service.DailyBalanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cash-closings")
public class CashClosingController {
    private final CashClosingService service;
    private final DailyBalanceService balances;

    public CashClosingController(CashClosingService service, DailyBalanceService balances) {
        this.service = service;
        this.balances = balances;
    }

    @PostMapping
    public ResponseEntity<CashClosingService.Closing> close(@RequestBody CashClosingService.CloseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.close(request));
    }

    @PostMapping("/open")
    public ResponseEntity<CashClosingService.OpenedCashDay> open(
            @RequestBody CashClosingService.OpenRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.open(request));
    }

    @PostMapping("/{id}/rectifications")
    public CashClosingService.Closing rectify(@PathVariable String id,
            @RequestBody CashClosingService.RectificationRequest request) {
        return service.rectify(id, request);
    }

    @GetMapping("/balance")
    public DailyBalanceService.DailyBalance balance(@RequestParam String dia,
            @RequestParam(required = false) String caja) {
        return balances.get(dia, caja);
    }
}
