package com.acmeair.mongo;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.bson.Document;
import org.json.JSONObject;

import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

@ApplicationScoped
public class MongoSessionCoordinator {

    private MongoCollection<Document> coordinatorCollection;
    MongoDatabase database;
    @Inject
    MongoClient mongoClient;

    private static final Logger logger = Logger.getLogger(MongoSessionCoordinator.class.getName());

    @PostConstruct
    public void initialization() {
        database = mongoClient.getDatabase("acmeair-bookingdb");
        coordinatorCollection = database.getCollection("coordinator");
    }

    public void initTransaction(String transactionId) {
        Document transactionDoc = new Document("_id", transactionId)
                .append("customerStatus", null)
                .append("bookingStatus", null)
                .append("done", "false");

        coordinatorCollection.insertOne(transactionDoc);
        logger.warning("initialized transaction: " + transactionId);

    }

    public void setPrepped(String transactionId, String type) {
        coordinatorCollection.updateOne(eq("_id", transactionId),
                set(type, "PREPPED"));
        logger.warning("setPrepped for transaction: " + transactionId + " and type: " + type);
    }

    public void setFailed(String transactionId, String type) {
        coordinatorCollection.updateOne(eq("_id", transactionId),
                set(type, "FAILED"));
        logger.warning("setFailed for transaction: " + transactionId + " and type: " + type);
    }

    public void setCommitted(String transactionId, String type) {
        coordinatorCollection.updateOne(eq("_id", transactionId),
                set(type, "COMMITTED"));
        logger.warning("setCommitted for transaction: " + transactionId + " and type: " + type);
    }

    public void setDone(String transactionId) {
        coordinatorCollection.updateOne(eq("_id", transactionId),
                set("done", "true"));
        logger.warning("setDone for transaction: " + transactionId);
    }

    public void setCommittedDone(String transactionId) {
        coordinatorCollection.updateOne(eq("_id", transactionId),
                combine(set("done", "true"),
                        set("customerStatus", "COMMITTED"),
                        set("bookingStatus", "COMMITTED")));
        logger.warning("setDone and all committed for transaction: " + transactionId);
    }

    public boolean allPrepCallsReturnedOk (String transactionId) {
        Document transactionDoc = coordinatorCollection.find(eq("_id", transactionId)).first();
        JSONObject transaction = new JSONObject(transactionDoc.toJson());

        return transaction.getString("customerStatus").equals("PREPPED") && transaction.getString("bookingStatus").equals("PREPPED");
    }

}
