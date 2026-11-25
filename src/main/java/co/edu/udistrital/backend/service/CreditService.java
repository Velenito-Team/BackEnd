package co.edu.udistrital.backend.service;

import com.mongodb.client.MongoClient;
import java.text.Normalizer;
import java.util.*;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Updates.*;

@Service
public class CreditService {
    private final MongoClient client;
    private final MongoTemplate mongo;
    private final OperatorService operator;

    public CreditService(MongoClient client, MongoTemplate mongo, OperatorService operator) {
        this.client = client;
        this.mongo = mongo;
        this.operator = operator;
    }

    public record Customer(String id, String nombre, String telefono, long saldo) {
    }

    public record CustomerRequest(String nombre, String telefono, boolean autorizacion, String evidencia) {
    }

    public List<Customer> customers() {
        return mongo.getCollection("clientesFiado")
                .find(new Document("autorizacionVigenteId", new Document("$exists", true)))
                .sort(new Document("nombreNormalizado", 1)).map(d -> new Customer(d.getObjectId("_id").toHexString(),
                        d.getString("nombre"), d.getString("telefono"), d.getLong("saldo")))
                .into(new ArrayList<>());
    }

    public Customer create(CustomerRequest request) {
        if (request.nombre() == null || request.nombre().isBlank() || !request.autorizacion()
                || request.evidencia() == null || request.evidencia().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Registra nombre y constancia de autorización previa del cliente.");
        var actor = operator.id();
        var id = new ObjectId();
        var authorization = new ObjectId();
        var now = new Date();
        String name = request.nombre().strip();
        var customer = new Document("_id", id).append("nombre", name)
                .append("nombreNormalizado",
                        Normalizer.normalize(name, Normalizer.Form.NFC).replaceAll("(?U)\\s+", " ")
                                .toLowerCase(Locale.ROOT))
                .append("saldo", 0L).append("autorizacionVigenteId", authorization).append("por", actor)
                .append("fecha", now);
        if (request.telefono() != null && !request.telefono().isBlank())
            customer.append("telefono", request.telefono().strip());
        try (var session = client.startSession()) {
            session.withTransaction(() -> {
                mongo.getCollection("autorizacionesDatos").insertOne(session, new Document("_id", authorization)
                        .append("clienteId", id).append("base", "consentimiento").append("decision", "concedida")
                        .append("finalidad", "Gestión de ventas fiadas, abonos y consulta de saldos")
                        .append("versionAviso", "fiados-v1").append("evidenciaRef", request.evidencia().strip())
                        .append("por", actor).append("fecha", now));
                mongo.getCollection("clientesFiado").insertOne(session, customer);
                return true;
            });
        }
        return new Customer(id.toHexString(), name, customer.getString("telefono"), 0);
    }
}
