package co.edu.udistrital.backend.service;

import co.edu.udistrital.backend.model.PaymentMethod;
import com.mongodb.MongoCommandException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import co.edu.udistrital.backend.repository.SaleRepository;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;
import static java.nio.charset.StandardCharsets.UTF_8;

@Service
public class SaleService {
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final String STORE = "Tienda El Veleñito";
    private final SaleRepository repository;
    private final String operator;
    private final String receiptSecret;

    public SaleService(SaleRepository repository,
            @Value("${velenito.pos.user:}") String operator,
            @Value("${velenito.receipt.secret:}") String receiptSecret) {
        this.repository = repository;
        this.operator = operator;
        this.receiptSecret = receiptSecret;
    }

    public record ReceiptLine(String productoId, String nombre, String unidad,
            BigDecimal cantidad, BigDecimal precioUnitario, long subtotal) {
    }

    public record Receipt(String id, Instant fecha, String tienda, String metodo, String estado,
            long total, Long cambio, List<ReceiptLine> lineas) {
    }

    public record CreatedReceipt(Receipt comprobante, String token) {
    }

    public record RequestedLine(String productoId, BigDecimal cantidad) {
    }

    public record InvoiceRequest(List<RequestedLine> lineas, PaymentMethod metodo, BigDecimal recibido,
            String clienteId) {
    }

    public CreatedReceipt saveInvoice(InvoiceRequest sale, UUID idempotencyKey) {
        var quantities = quantities(sale);
        var db = repository.database();
        var hello = db.runCommand(new Document("hello", 1));
        if (!hello.containsKey("setName") && !"isdbgrid".equals(hello.getString("msg"))) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "MongoDB debe estar en un replica set para confirmar ventas y actualizar inventario.");
        }
        if (operator.isBlank() || receiptSecret.getBytes(UTF_8).length < 32) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configura el usuario operador y el secreto del comprobante en el servidor.");
        }
        final Long cashReceived = sale.recibido() == null ? null : amount(sale.recibido());
        final String requestHash = sha256(sale.metodo() + ":"
                + (cashReceived == null ? "" : cashReceived)
                + ":" + sale.clienteId() + ":" + quantities);
        var sales = repository.collection("ventas");
        var ops = repository.collection("operaciones");
        var cashier = repository.collection("usuarios").find(and(eq("activo", true),
                eq("usuarioNormalizado", operator), in("rol", "administrador", "cajero"))).first();
        if (cashier == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El operador de caja no está activo.");
        }
        ObjectId actor = cashier.getObjectId("_id");
        var key = and(eq("por", actor), eq("tipo", "confirmar_venta"), eq("clave", idempotencyKey.toString()));
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String day = LocalDate.ofInstant(now, BOGOTA).toString();
        ObjectId saleId = new ObjectId();
        String token = token(saleId);
        var dayKey = and(eq("caja", "01"), eq("dia", day));
        var days = repository.collection("cajasDia");
        try (var session = repository.client().startSession()) {
            try {
                return session.withTransaction(() -> {
                    var existing = ops.find(session, key).first();
                    if (existing != null)
                        return existingReceipt(existing, requestHash);
                    var cashDay = days.findOneAndUpdate(session, dayKey,
                            combine(setOnInsert("caja", "01"), setOnInsert("dia", day),
                                    setOnInsert("estado", "abierta"), inc("version", 1)),
                            new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
                    if (!"abierta".equals(cashDay.getString("estado"))) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "La caja del día ya está cerrada.");
                    }
                    var products = repository.collection("productos");
                    var lines = new ArrayList<Document>();
                    long total = 0;
                    for (var entry : quantities.entrySet()) {
                        var quantity = entry.getValue();
                        var product = products.findOneAndUpdate(session,
                                and(eq("_id", entry.getKey()), eq("activo", true),
                                        gte("stock", new Decimal128(quantity))),
                                combine(inc("stock", new Decimal128(quantity.negate())),
                                        set("actualizadoEn", Date.from(now))),
                                new FindOneAndUpdateOptions().returnDocument(ReturnDocument.BEFORE));
                        if (product == null) {
                            throw new ResponseStatusException(HttpStatus.CONFLICT,
                                    "Producto inactivo o stock insuficiente.");
                        }
                        if (!product.getBoolean("fraccionable", false) && quantity.stripTrailingZeros().scale() > 0) {
                            throw badRequest(
                                    "El producto " + product.getString("nombre") + " se vende por unidad entera.");
                        }
                        var price = product.get("precioVenta", Decimal128.class);
                        long subtotal = amount(
                                price.bigDecimalValue().multiply(quantity).setScale(0, RoundingMode.HALF_UP));
                        try {
                            total = Math.addExact(total, subtotal);
                        } catch (ArithmeticException ex) {
                            throw badRequest("El total excede el rango permitido.");
                        }
                        lines.add(
                                new Document("productoId", entry.getKey()).append("nombre", product.getString("nombre"))
                                        .append("unidad", product.getString("unidad"))
                                        .append("cantidad", new Decimal128(quantity))
                                        .append("precioUnitario", price).append("subtotal", subtotal));
                    }
                    ObjectId dayId = cashDay.getObjectId("_id");
                    String method = sale.metodo().name();
                    var record = new Document("_id", saleId).append("cajaDiaId", dayId).append("dia", day)
                            .append("por", actor).append("fecha", Date.from(now)).append("lineas", lines)
                            .append("total", total).append("metodo", method).append("estado", "confirmada")
                            .append("comprobante", new Document("tienda", STORE).append("emitidoEn", Date.from(now)));
                    if (sale.metodo() == PaymentMethod.efectivo) {
                        long received = cashReceived == null ? total : cashReceived;
                        if (received < total)
                            throw badRequest("El efectivo recibido es menor que el total.");
                        record.append("recibido", received).append("cambio", received - total);
                    }
                    if (sale.metodo() == PaymentMethod.fiado) {
                        var customerId = new ObjectId(sale.clienteId());
                        var customer = repository.collection("clientesFiado").find(session, eq("_id", customerId)).first();
                        if (customer == null || customer.getObjectId("autorizacionVigenteId") == null)
                            throw badRequest("El cliente requiere autorización vigente.");
                        var authorization = repository.collection("autorizacionesDatos").find(session,
                                and(eq("_id", customer.getObjectId("autorizacionVigenteId")),
                                        eq("clienteId", customerId), eq("decision", "concedida")))
                                .first();
                        if (authorization == null)
                            throw badRequest("La autorización del cliente no está vigente.");
                        repository.collection("clientesFiado").updateOne(session, eq("_id", customerId),
                                inc("saldo", total));
                        repository.collection("deudasFiado").insertOne(session, new Document("ventaId", saleId)
                                .append("clienteId", customerId).append("importeOriginal", total).append("saldo", total)
                                .append("estado", total == 0 ? "pagada" : "pendiente").append("fecha", Date.from(now)));
                        record.append("clienteId", customerId);
                    }
                    sales.insertOne(session, record);
                    if (total > 0 && sale.metodo() != PaymentMethod.fiado) {
                        repository.collection("movimientosCaja").insertOne(session,
                                new Document("cajaDiaId", dayId).append("dia", day).append("por", actor)
                                        .append("fecha", Date.from(now)).append("tipo", "venta").append("medio", method)
                                        .append("monto", total).append("ventaId", saleId));
                    }
                    repository.collection("enlacesComprobante").insertOne(session,
                            new Document("ventaId", saleId).append("tokenHash", sha256(token))
                                    .append("emitidoEn", Date.from(now))
                                    .append("expiraEn", Date.from(now.plus(30, ChronoUnit.DAYS))));
                    ops.insertOne(session, new Document("por", actor).append("tipo", "confirmar_venta")
                            .append("clave", idempotencyKey.toString()).append("solicitudHash", requestHash)
                            .append("resultadoId", saleId).append("fecha", Date.from(now)));
                    return new CreatedReceipt(receipt(record), token);
                });
            } catch (MongoWriteException error) {
                if (error.getCode() != 11000)
                    throw error;
                var existing = ops.find(key).first();
                if (existing == null)
                    throw saleConflict();
                return existingReceipt(existing, requestHash);
            } catch (MongoCommandException error) {
                if (error.getCode() == 11000)
                    throw saleConflict();
                if (error.getCode() != 20)
                    throw error;
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "El servidor no admite transacciones.");
            }
        }
    }

    public Receipt readReceipt(String token) {
        if (!token.matches("[a-f0-9]{64}"))
            throw missingReceipt();
        var link = repository.collection("enlacesComprobante")
                .find(and(eq("tokenHash", sha256(token)), gt("expiraEn", new Date()))).first();
        if (link == null)
            throw missingReceipt();
        var sale = repository.collection("ventas").find(eq("_id", link.getObjectId("ventaId"))).first();
        if (sale == null)
            throw missingReceipt();
        return receipt(sale);
    }

    private CreatedReceipt existingReceipt(Document operation, String requestHash) {
        if (!requestHash.equals(operation.getString("solicitudHash"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La clave ya se usó para otra venta.");
        }
        ObjectId id = operation.getObjectId("resultadoId");
        var sale = repository.collection("ventas").find(eq("_id", id)).first();
        if (sale == null)
            throw missingReceipt();
        return new CreatedReceipt(receipt(sale), token(id));
    }

    static Receipt receipt(Document sale) {
        var lines = sale.getList("lineas", Document.class).stream()
                .map(line -> new ReceiptLine(line.getObjectId("productoId").toHexString(), line.getString("nombre"),
                        line.getString("unidad"), line.get("cantidad", Decimal128.class).bigDecimalValue(),
                        line.get("precioUnitario", Decimal128.class).bigDecimalValue(), line.getLong("subtotal")))
                .toList();
        return new Receipt(sale.getObjectId("_id").toHexString(), sale.getDate("fecha").toInstant(),
                sale.get("comprobante", Document.class).getString("tienda"), sale.getString("metodo"),
                sale.getString("estado"), sale.getLong("total"), sale.getLong("cambio"), lines);
    }

    private static Map<ObjectId, BigDecimal> quantities(InvoiceRequest sale) {
        if (sale == null || sale.lineas() == null || sale.lineas().isEmpty() || sale.metodo() == null) {
            throw badRequest("Indica los productos y el método de pago.");
        }
        if (sale.metodo() == PaymentMethod.fiado && (sale.clienteId() == null || !ObjectId.isValid(sale.clienteId())))
            throw badRequest("Selecciona un cliente autorizado para la venta fiada.");
        if (sale.metodo() != PaymentMethod.efectivo && sale.recibido() != null) {
            throw badRequest("El monto recibido solo aplica a pagos en efectivo.");
        }
        var quantities = new LinkedHashMap<ObjectId, BigDecimal>();
        for (var line : sale.lineas()) {
            if (line == null || line.productoId() == null || !ObjectId.isValid(line.productoId())
                    || line.cantidad() == null || line.cantidad().signum() <= 0
                    || line.cantidad().stripTrailingZeros().scale() > 3
                    || line.cantidad().compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
                throw badRequest(
                        "Cada línea debe incluir un producto y una cantidad positiva con hasta tres decimales.");
            }
            var quantity = line.cantidad().stripTrailingZeros();
            quantities.merge(new ObjectId(line.productoId()), quantity, BigDecimal::add);
        }
        return quantities;
    }

    private static long amount(BigDecimal amount) {
        try {
            if (amount.signum() < 0)
                throw new ArithmeticException();
            return amount.longValueExact();
        } catch (ArithmeticException error) {
            throw badRequest("El importe debe expresarse en pesos colombianos enteros dentro del rango permitido.");
        }
    }

    private String token(ObjectId saleId) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(receiptSecret.getBytes(UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(saleId.toByteArray()));
        } catch (java.security.GeneralSecurityException error) {
            throw new IllegalStateException("No se pudo generar el token del comprobante.", error);
        }
    }

    static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(UTF_8)));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ResponseStatusException saleConflict() {
        return new ResponseStatusException(HttpStatus.CONFLICT,
                "Otra operación actualizó la caja. Reintenta la venta.");
    }

    private static ResponseStatusException missingReceipt() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no disponible.");
    }

    CreatedReceipt renew(ObjectId id, ObjectId actor) {
        var sale = repository.collection("ventas").find(eq("_id", id)).first();
        if (sale == null)
            throw missingReceipt();
        if (receiptSecret.getBytes(UTF_8).length < 32)
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Configura el secreto del comprobante.");
        String accessToken = token(id);
        var now = Instant.now();
        repository.collection("enlacesComprobante").updateOne(eq("ventaId", id),
                combine(set("ventaId", id), set("tokenHash", sha256(accessToken)),
                        set("emitidoEn", Date.from(now)), set("expiraEn", Date.from(now.plus(30, ChronoUnit.DAYS)))),
                new com.mongodb.client.model.UpdateOptions().upsert(true));
        return new CreatedReceipt(receipt(sale), accessToken);
    }
}
