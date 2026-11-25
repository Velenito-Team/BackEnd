package co.edu.udistrital.backend.controller;

import com.mongodb.MongoException;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> business(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",
                ex.getReason() == null ? "No se pudo completar la operación." : ex.getReason()));
    }

    @ExceptionHandler({ IllegalArgumentException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class })
    ResponseEntity<Map<String, String>> invalid(Exception ex) {
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Revisa los campos, cantidades, fechas e identificadores."));
    }

    @ExceptionHandler(DuplicateKeyException.class)
    ResponseEntity<Map<String, String>> duplicate(Exception ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Ya existe un registro con esos datos."));
    }

    @ExceptionHandler(MongoException.class)
    ResponseEntity<Map<String, String>> database(MongoException ex) {
        var status = ex.getCode() == 11000 ? HttpStatus.CONFLICT : HttpStatus.SERVICE_UNAVAILABLE;
        var message = ex.getCode() == 11000 ? "La operación ya existe. Actualiza y vuelve a intentarlo."
                : "No se pudo completar la operación en la base de datos. Puedes reintentar.";
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
