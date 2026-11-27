package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.repository.CreditRepository;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class CreditServiceTest {
    private final CreditRepository repository = mock(CreditRepository.class);
    private final CreditService service = new CreditService(repository, mock(OperatorService.class));

    @Test
    void listsCustomerDebtsJoinedToSaleDetails() {
        ObjectId customerId = new ObjectId();
        ObjectId debtId = new ObjectId();
        ObjectId saleId = new ObjectId();
        ObjectId productId = new ObjectId();
        Date saleDate = new Date();
        Document debt = new Document("_id", debtId).append("ventaId", saleId).append("saldo", 4200L);
        Document line = new Document("productoId", productId).append("nombre", "Arroz").append("unidad", "kg")
                .append("cantidad", new Decimal128(BigDecimal.ONE))
                .append("precioUnitario", new Decimal128(BigDecimal.valueOf(4200)))
                .append("subtotal", 4200L);
        Document sale = new Document("_id", saleId).append("fecha", saleDate).append("total", 4200L)
                .append("estado", "confirmada").append("lineas", List.of(line));
        when(repository.findDebtsByCustomer(customerId)).thenReturn(List.of(debt));
        when(repository.findSalesByIds(List.of(saleId))).thenReturn(List.of(sale));

        List<CreditService.Credit> result = service.credits(customerId.toHexString());

        assertEquals(1, result.size());
        assertEquals(debtId.toHexString(), result.getFirst().id());
        assertEquals(saleDate.toInstant(), result.getFirst().fecha());
        assertEquals(4200L, result.getFirst().total());
        assertEquals(4200L, result.getFirst().saldo());
        assertEquals("confirmada", result.getFirst().estado());
        assertEquals(productId.toHexString(), result.getFirst().lineas().getFirst().productoId());
        verify(repository).findSalesByIds(List.of(saleId));
    }

    @Test
    void rejectsInvalidCustomerIdBeforeQueryingRepository() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.credits("not-an-object-id"));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository);
    }
}
