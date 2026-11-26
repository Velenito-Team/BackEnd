package co.edu.udistrital.backend.repository;

import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Sorts.ascending;
import com.mongodb.client.MongoClient;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CreditRepository {
    private final MongoClient client;
    private final MongoTemplate mongo;
    public CreditRepository(MongoClient client, MongoTemplate mongo) { this.client = client; this.mongo = mongo; }
    public List<Document> findAuthorizedCustomers() {
        return mongo.getCollection("clientesFiado").find(exists("autorizacionVigenteId"))
                .sort(ascending("nombreNormalizado")).into(new ArrayList<>());
    }
    public void saveCustomerWithAuthorization(Document customer, Document authorization) {
        try (var session = client.startSession()) {
            session.withTransaction(() -> {
                mongo.getCollection("autorizacionesDatos").insertOne(session, authorization);
                mongo.getCollection("clientesFiado").insertOne(session, customer);
                return true;
            });
        }
    }
}
