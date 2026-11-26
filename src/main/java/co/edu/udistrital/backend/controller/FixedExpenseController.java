package co.edu.udistrital.backend.controller;

import co.edu.udistrital.backend.service.FixedExpenseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/fixed-expenses")
public class FixedExpenseController {
    private final FixedExpenseService service;

    public FixedExpenseController(FixedExpenseService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FixedExpenseService.FixedExpense create(@RequestBody FixedExpenseService.FixedExpenseRequest request) {
        return service.save(request);
    }
}
