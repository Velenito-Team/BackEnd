package co.edu.udistrital.backend.controller;

import co.edu.udistrital.backend.service.ProviderService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {
    private final ProviderService service;

    public ProviderController(ProviderService service) { this.service = service; }

    @GetMapping
    public List<ProviderService.ProviderView> list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProviderService.ProviderView create(@RequestBody ProviderService.CreateRequest request) {
        return service.create(request);
    }
}
