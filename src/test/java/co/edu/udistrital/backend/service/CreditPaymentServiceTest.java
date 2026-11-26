package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.udistrital.backend.repository.CreditPaymentRepository;
import java.util.Date;
import java.util.Optional;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CreditPaymentServiceTest {
    private CreditPaymentRepository repository;
    private OperatorService operator;
    private CreditPaymentService service;
    private ObjectId customerId;
    private ObjectId debtId;
    private ObjectId cashDayId;

    @BeforeEach
    void setUp() {
        repository = mock(CreditPaymentRepository.class);
        operator = mock(OperatorService.class);
        service = new CreditPaymentService(repository, operator);
        customerId = new ObjectId();
        debtId = new ObjectId();
        cashDayId = new ObjectId();
        when(operator.id()).thenReturn(new ObjectId());
    }

    @Test
    void registraAbonoYDevuelveComprobante() {
        when(repository.findOpenCashDay(eq("01"), any())).thenReturn(Optional.of(new Document("_id", cashDayId)));
        Date now = new Date();
        when(repository.savePayment(eq(customerId), eq(debtId), eq(cashDayId), any(), any(), eq("efectivo"),
                eq(30000L), any())).thenReturn(new Document("_id", new ObjectId()).append("monto", 30000L)
                        .append("medio", "efectivo").append("saldoRestante", 20000L).append("emitidoEn", now));

        CreditPaymentService.PaymentReceipt receipt = service.pay(customerId.toHexString(),
                new CreditPaymentService.PaymentRequest(debtId.toHexString(), 30000L, "efectivo"));

        assertEquals(30000L, receipt.monto());
        assertEquals(20000L, receipt.saldoRestante());
        verify(repository).savePayment(eq(customerId), eq(debtId), eq(cashDayId), any(), any(), eq("efectivo"),
                eq(30000L), any());
    }

    @Test
    void rechazaAbonoExcesivo() {
        when(repository.findOpenCashDay(eq("01"), any())).thenReturn(Optional.of(new Document("_id", cashDayId)));
        when(repository.savePayment(any(), any(), any(), any(), any(), any(), eq(50001L), any()))
                .thenThrow(new CreditPaymentRepository.PaymentException("EXCESO"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.pay(
                customerId.toHexString(), new CreditPaymentService.PaymentRequest(debtId.toHexString(), 50001L, "nequi")));

        assertEquals(400, error.getStatusCode().value());
    }

    @Test
    void rechazaClienteSinDeuda() {
        when(repository.findOpenCashDay(eq("01"), any())).thenReturn(Optional.of(new Document("_id", cashDayId)));
        when(repository.savePayment(any(), any(), any(), any(), any(), any(), any(Long.class), any()))
                .thenThrow(new CreditPaymentRepository.PaymentException("NO_DEUDA"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.pay(
                customerId.toHexString(), new CreditPaymentService.PaymentRequest(debtId.toHexString(), 1000L, "daviplata")));

        assertEquals(409, error.getStatusCode().value());
    }
}
