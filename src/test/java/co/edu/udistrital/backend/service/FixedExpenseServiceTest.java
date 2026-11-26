package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.repository.FixedExpenseRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class FixedExpenseServiceTest {
    private FixedExpenseRepository repository;
    private OperatorService operator;
    private FixedExpenseService service;

    @BeforeEach
    void setUp() {
        repository = mock(FixedExpenseRepository.class);
        operator = mock(OperatorService.class);
        service = new FixedExpenseService(repository, operator,
                Clock.fixed(Instant.parse("2026-11-26T15:00:00Z"), ZoneId.of("America/Bogota")));
        when(operator.current()).thenReturn(new Document("_id", new ObjectId()).append("rol", "administrador"));
    }

    @Test
    void guardaGastoYCalculaResumenDelMes() {
        ObjectId id = new ObjectId();
        when(repository.save(eq("Arriendo"), eq("arriendo"), eq("2026-11"), eq(800000L), eq(5), any(), any()))
                .thenReturn(new Document("_id", id).append("concepto", "Arriendo").append("monto", 800000L)
                        .append("mes", "2026-11").append("diaRecordatorio", 5));
        when(repository.findByMonth("2026-11")).thenReturn(List.of(
                new Document("_id", id).append("concepto", "Arriendo").append("monto", 800000L)
                        .append("mes", "2026-11").append("diaRecordatorio", 5)));

        service.save(new FixedExpenseService.FixedExpenseRequest("Arriendo", 800000L, "2026-11", 5));
        var summary = service.summary("2026-11");

        assertEquals(800000L, summary.total());
        assertEquals(1, summary.gastos().size());
    }

    @Test
    void marcaRecordatorioCuandoLaFechaSeAproxima() {
        ObjectId id = new ObjectId();
        when(repository.findByMonth("2026-11")).thenReturn(List.of(
                new Document("_id", id).append("concepto", "Internet").append("monto", 100000L)
                        .append("mes", "2026-11").append("diaRecordatorio", 28)));

        var summary = service.summary("2026-11");

        assertEquals(true, summary.recordatorios().getFirst().pendiente());
    }

    @Test
    void rechazaMesOMontoInvalidos() {
        assertThrows(ResponseStatusException.class, () -> service.save(
                new FixedExpenseService.FixedExpenseRequest("Luz", 0, "2026-11", 10)));
        assertThrows(ResponseStatusException.class, () -> service.save(
                new FixedExpenseService.FixedExpenseRequest("Luz", 1000, "2026-13", 10)));
    }
}
