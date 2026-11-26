package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.repository.InventoryRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

class InventoryServiceTest {
    @Test
    void permiteIngresoSinProveedor() {
        var repository = mock(InventoryRepository.class);
        var operator = mock(OperatorService.class);
        ObjectId productId = new ObjectId();
        ObjectId actor = new ObjectId();
        UUID key = UUID.randomUUID();
        when(operator.id()).thenReturn(actor);
        when(repository.receive(eq(productId), eq(new BigDecimal("2")), eq(new BigDecimal("1000")),
                isNull(), eq(actor), eq(key), anyString())).thenReturn("reception-id");

        String result = new InventoryService(repository, operator).receive(
                new InventoryService.Entry(productId.toHexString(), new BigDecimal("2"), new BigDecimal("1000"), null), key);

        assertEquals("reception-id", result);
    }
}
