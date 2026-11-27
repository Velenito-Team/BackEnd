package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import co.edu.udistrital.backend.model.PaymentMethod;
import co.edu.udistrital.backend.repository.SaleRepository;
import com.mongodb.client.MongoDatabase;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SaleServiceTest {
    @Test
    void rejectsStandaloneBeforeWritingAnySaleOrStock() {
        var repository = mock(SaleRepository.class);
        var db = mock(MongoDatabase.class);
        when(repository.database()).thenReturn(db);
        when(db.runCommand(new Document("hello", 1))).thenReturn(new Document("ok", 1.0));
        var line = new SaleService.RequestedLine("507f1f77bcf86cd799439011", BigDecimal.ONE);
        var sale = new SaleService.InvoiceRequest(List.of(line), PaymentMethod.efectivo, null, null);
        var service = new SaleService(repository, "", "");
        var error = assertThrows(ResponseStatusException.class, () -> service.saveInvoice(sale, UUID.randomUUID()));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verify(db).runCommand(new Document("hello", 1));
        verifyNoMoreInteractions(db);
    }
}
