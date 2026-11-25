package co.edu.udistrital.backend.controller;

import co.edu.udistrital.backend.service.CreditService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CreditController {
    private final CreditService service;

    public CreditController(CreditService service) {
        this.service = service;
    }

    @GetMapping
    public List<CreditService.Customer> list() {
        return service.customers();
    }

    @PostMapping
    public CreditService.Customer create(@RequestBody CreditService.CustomerRequest request) {
        return service.create(request);
    }
}
