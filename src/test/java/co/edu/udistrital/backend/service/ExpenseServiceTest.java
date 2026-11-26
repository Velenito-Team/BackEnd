package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.repository.ExpenseRepository;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {
    @Mock ExpenseRepository repository;
    @Mock OperatorService operator;

    @Test
    void savesValidExpenseForAccountingOperator() {
        ObjectId actor = new ObjectId();
        ObjectId cashDay = new ObjectId();
        when(operator.current()).thenReturn(new Document("_id", actor).append("rol", "administrador"));
        when(repository.findOpenCashDay("01", "2026-11-26"))
                .thenReturn(Optional.of(new Document("_id", cashDay)));
        when(repository.save(any(), any(), any(), any(), any(Long.class), any(), any(), any()))
                .thenAnswer(invocation -> new Document("_id", new ObjectId()).append("cajaDiaId", cashDay)
                        .append("dia", "2026-11-26").append("medio", "efectivo").append("monto", 50000L)
                        .append("descripcion", "Compra a proveedor"));

        ExpenseService.Expense saved = new ExpenseService(repository, operator).save(
                new ExpenseService.ExpenseRequest("2026-11-26", "efectivo", 50000, "Compra a proveedor", null));

        assertEquals(50000, saved.monto());
        verify(repository).save(any(), any(), any(), any(), any(Long.class), any(), any(), any());
    }

    @Test
    void rejectsZeroOrNegativeAmount() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> new ExpenseService(repository, operator).save(
                        new ExpenseService.ExpenseRequest("2026-11-26", "efectivo", 0, "Compra", null)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void rejectsMissingDescription() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> new ExpenseService(repository, operator).save(
                        new ExpenseService.ExpenseRequest("2026-11-26", "efectivo", 1000, " ", null)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }
}
