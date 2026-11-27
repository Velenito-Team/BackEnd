package co.edu.udistrital.backend.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SaleRepository {
    private final MongoClient client;
    private final MongoTemplate mongo;

    public SaleRepository(MongoClient client, MongoTemplate mongo) {
        this.client = client;
        this.mongo = mongo;
    }

    public MongoClient client() { return client; }
    public MongoDatabase database() { return mongo.getDb(); }
    public MongoCollection<Document> collection(String name) { return mongo.getCollection(name); }
}
