package io.github.zaratath.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import io.github.zaratath.BcordCraft;
import org.bson.UuidRepresentation;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class Mongo {
    private static MongoClient client;
    private static MongoDatabase database;

    public static MongoDatabase getDatabase() {
        if(database != null) return database;

        try {
            ConnectionString connectionString = new ConnectionString(
                    BcordCraft.getInstance().getConfig().getString("connectionString"));

            MongoClientSettings settings = MongoClientSettings.builder()
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .applyConnectionString(connectionString)
                    .build();

            client = MongoClients.create(settings);
            database = client.getDatabase("BcordCraft");
        }
        catch(Exception e){
            Bukkit.getLogger().log(Level.SEVERE, "Failed to connect to database. Stopping server.");
            e.printStackTrace();
            Bukkit.shutdown();
        }

        return database;

    }
}