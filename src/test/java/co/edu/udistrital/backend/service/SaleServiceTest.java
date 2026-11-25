package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import co.edu.udistrital.backend.model.PaymentMethod;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class SaleServiceTest {
    @Test
    void rejectsStandaloneBeforeWritingAnySaleOrStock() {
        var client = mock(MongoClient.class);
        var template = mock(MongoTemplate.class);
        var db = mock(MongoDatabase.class);
        when(template.getDb()).thenReturn(db);
        when(db.runCommand(new Document("hello", 1))).thenReturn(new Document("ok", 1.0));
        var line = new SaleService.RequestedLine("507f1f77bcf86cd799439011", BigDecimal.ONE);
        var sale = new SaleService.InvoiceRequest(List.of(line), PaymentMethod.efectivo, null, null);
        var service = new SaleService(client, template, "", "");
        var error = assertThrows(ResponseStatusException.class, () -> service.saveInvoice(sale, UUID.randomUUID()));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, error.getStatusCode());
        verify(db).runCommand(new Document("hello", 1));
        verifyNoMoreInteractions(db);
        verifyNoInteractions(client);
    }
}
