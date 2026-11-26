package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import co.edu.udistrital.backend.model.Product;
import co.edu.udistrital.backend.model.Unit;
import co.edu.udistrital.backend.repository.ProductRepository;
import co.edu.udistrital.backend.repository.AuditRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository repository;
    @Mock OperatorService operator;
    @Mock AuditRepository audits;
    @InjectMocks ProductService service;

    @Test
    void createsAValidProduct() {
        Product product = new Product("Arroz", "Granos", BigDecimal.ONE, BigDecimal.TEN, Unit.kg);
        when(operator.id()).thenReturn(new org.bson.types.ObjectId());
        when(repository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(new org.bson.types.ObjectId().toHexString());
            return saved;
        });
        Product saved = service.create(product);
        assertEquals("arroz", saved.getNombreNormalizado());
        verify(repository).save(product);
    }

    @Test
    void deletesProductSoftly() {
        Product product = new Product("Pan", "Panadería", BigDecimal.ONE, BigDecimal.TEN, Unit.unidad);
        product.setId(new org.bson.types.ObjectId().toHexString());
        when(operator.id()).thenReturn(new org.bson.types.ObjectId());
        when(repository.findById("1")).thenReturn(Optional.of(product));
        service.delete("1");
        verify(repository).save(product);
        assertEquals(false, product.isActivo());
    }

    @Test
    void rejectsFractionalStockForAnIndivisibleProduct() {
        Product product = new Product("Pan", "Panadería", BigDecimal.ONE, BigDecimal.TEN, Unit.unidad);
        product.setStock(new BigDecimal("0.5"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.create(product));
        org.mockito.Mockito.verifyNoInteractions(repository);
    }

    @Test
    void rejectsAnEditWithStaleStock() {
        Product current = new Product("Pan", "Panadería", BigDecimal.ONE, BigDecimal.TEN, Unit.unidad);
        current.setStock(BigDecimal.ONE);
        when(repository.findById("1")).thenReturn(Optional.of(current));
        Product edited = new Product("Pan", "Panadería", BigDecimal.ONE, BigDecimal.TEN, Unit.unidad);
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> service.update("1", edited));
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.never()).save(any());
    }
}
