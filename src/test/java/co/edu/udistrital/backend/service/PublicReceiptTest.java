package co.edu.udistrital.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import co.edu.udistrital.backend.repository.SaleRepository;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class PublicReceiptTest {
    @Test void publicFiadoProjectionExcludesCustomerAndPurchaseCosts() {
        var sale = sale("confirmada");
        var receipt = SaleService.receipt(sale);
        assertEquals("fiado", receipt.metodo());
        assertEquals(new BigDecimal("0.125"), receipt.lineas().getFirst().cantidad());
        assertEquals(1563, receipt.total());
        assertEquals(List.of("id", "fecha", "tienda", "metodo", "estado", "total", "cambio", "lineas"),
                Arrays.stream(SaleService.Receipt.class.getRecordComponents()).map(c -> c.getName()).toList());
        assertEquals(List.of("productoId", "nombre", "unidad", "cantidad", "precioUnitario", "subtotal"),
                Arrays.stream(SaleService.ReceiptLine.class.getRecordComponents()).map(c -> c.getName()).toList());
    }

    @Test @SuppressWarnings("unchecked") void validPublicLinkReturnsCancelledSaleWithoutOperatorLogin() {
        var repository = mock(SaleRepository.class);
        MongoCollection<Document> links = mock(MongoCollection.class), sales = mock(MongoCollection.class);
        FindIterable<Document> linkQuery = mock(FindIterable.class), saleQuery = mock(FindIterable.class);
        Document sale = sale("anulada");
        when(repository.collection("enlacesComprobante")).thenReturn(links);
        when(repository.collection("ventas")).thenReturn(sales);
        when(links.find(any(Bson.class))).thenReturn(linkQuery);
        when(linkQuery.first()).thenReturn(new Document("ventaId", sale.getObjectId("_id")));
        when(sales.find(any(Bson.class))).thenReturn(saleQuery);
        when(saleQuery.first()).thenReturn(sale);
        var receipt = new SaleService(repository, "", "").readReceipt("a".repeat(64));
        assertEquals("anulada", receipt.estado());
        assertEquals("Café", receipt.lineas().getFirst().nombre());
        verify(repository, never()).database(); verify(repository, never()).client();
    }

    @Test @SuppressWarnings("unchecked") void invalidUnknownAndExpiredLinksHaveGenericError() {
        var repository = mock(SaleRepository.class);
        MongoCollection<Document> links = mock(MongoCollection.class);
        FindIterable<Document> query = mock(FindIterable.class);
        when(repository.collection("enlacesComprobante")).thenReturn(links);
        when(links.find(any(Bson.class))).thenReturn(query);
        when(query.first()).thenReturn(null);
        var service = new SaleService(repository, "", "");
        for (String token : List.of("invalid", "a".repeat(64))) {
            var ex = assertThrows(ResponseStatusException.class, () -> service.readReceipt(token));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals("Comprobante no disponible.", ex.getReason());
        }
        verify(links).find(argThat((Bson filter) -> {
            String json = filter.toBsonDocument().toJson();
            return json.contains("tokenHash") && json.contains("expiraEn") && json.contains("$gt");
        }));
    }

    private static Document sale(String status) {
        return new Document("_id", new ObjectId()).append("fecha", Date.from(Instant.parse("2026-11-26T15:00:00Z")))
                .append("comprobante", new Document("tienda", "Tienda El Veleñito"))
                .append("metodo", "fiado").append("estado", status).append("total", 1563L)
                .append("clienteId", new ObjectId()).append("cliente", new Document("nombre", "Privado"))
                .append("lineas", List.of(new Document("productoId", new ObjectId()).append("nombre", "Café")
                        .append("unidad", "kg").append("cantidad", new Decimal128(new BigDecimal("0.125")))
                        .append("precioUnitario", new Decimal128(new BigDecimal("12500")))
                        .append("precioCompra", new Decimal128(new BigDecimal("9000"))).append("subtotal", 1563L)));
    }
}
