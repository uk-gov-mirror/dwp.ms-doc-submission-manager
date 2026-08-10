package uk.gov.dwp.health.pip.document.submission.manager.config;

import static uk.gov.dwp.health.pip.document.submission.manager.utils.EnvironmentUtil.getEnv;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

public class MongoClientConnection {
  public static MongoTemplate getMongoTemplate() {
    ConnectionString connectionString =
        new ConnectionString(
            "mongodb://"
                + getEnv("MONGODB_HOST", "localhost")
                + ":"
                + getEnv("`c`", "27017")
                + "/pip-apply-acc-mgr");

    MongoClientSettings mongoClientSettings =
        MongoClientSettings.builder().applyConnectionString(connectionString).build();

    MongoClient mongoClient = MongoClients.create(mongoClientSettings);

    return new MongoTemplate(mongoClient, "doc-sub-mgr-db");
  }

  public static void emptyMongoCollections() {
    getMongoTemplate().getCollection("document").deleteMany(new Document());
    getMongoTemplate().getCollection("drs_upload").deleteMany(new Document());
    getMongoTemplate().getCollection("submission").deleteMany(new Document());
  }
}
