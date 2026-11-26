package co.edu.udistrital.backend.controller;

import co.edu.udistrital.backend.service.CreditService;
import co.edu.udistrital.backend.service.CreditPaymentService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class CreditController {
    private final CreditService service;
    private final CreditPaymentService paymentService;

    public CreditController(CreditService service, CreditPaymentService paymentService) {
        this.service = service;
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<CreditService.Customer> list() {
        return service.customers();
    }

    @PostMapping
    public CreditService.Customer create(@RequestBody CreditService.CustomerRequest request) {
        return service.create(request);
    }

    @PostMapping("/{clienteId}/abonos")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public CreditPaymentService.PaymentReceipt pay(@PathVariable String clienteId,
            @RequestBody CreditPaymentService.PaymentRequest request) {
        return paymentService.pay(clienteId, request);
    }
}
