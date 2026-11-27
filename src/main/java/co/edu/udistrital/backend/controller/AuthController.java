package co.edu.udistrital.backend.controller;

import co.edu.udistrital.backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserService users;

    public AuthController(AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository, UserService users) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.users = users;
    }

    public record LoginRequest(String usuario, String contrasena) { }

    @PostMapping("/login")
    public UserService.UserView login(@RequestBody LoginRequest request,
            HttpServletRequest servletRequest, HttpServletResponse response) {
        if (request == null || request.usuario() == null || request.contrasena() == null)
            throw new BadCredentialsException("Usuario o contraseña incorrectos.");
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.usuario(), request.contrasena()));
        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, response);
        return users.findByUsername(authentication.getName());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
