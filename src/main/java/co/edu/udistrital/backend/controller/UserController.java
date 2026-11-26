package co.edu.udistrital.backend.controller;

import co.edu.udistrital.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserService.UserView create(@RequestBody UserService.CreateRequest request) {
        return users.create(request);
    }

    @GetMapping
    public java.util.List<UserService.UserView> list() {
        return users.list();
    }

    @PatchMapping("/{id}/status")
    public UserService.UserView status(@PathVariable String id,
            @RequestBody UserService.ActiveRequest request) {
        if (request == null) throw new org.springframework.web.server.ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Indica el estado activo del usuario.");
        return users.setActive(id, request.activo());
    }

    @PatchMapping("/{id}/role")
    public UserService.UserView role(@PathVariable String id,
            @RequestBody UserService.RoleRequest request) {
        return users.setRole(id, request == null ? null : request.rol());
    }
}
