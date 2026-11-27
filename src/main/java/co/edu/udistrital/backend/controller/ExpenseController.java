package co.edu.udistrital.backend.controller;

import co.edu.udistrital.backend.service.ExpenseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {
    private final ExpenseService service;

    public ExpenseController(ExpenseService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ExpenseService.Expense> create(@RequestBody ExpenseService.ExpenseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(request));
    }
}
