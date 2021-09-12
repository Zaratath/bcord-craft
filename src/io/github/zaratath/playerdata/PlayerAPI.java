package io.github.zaratath.playerdata;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.zaratath.database.Mongo;
import io.github.zaratath.playerdata.guilds.Guild;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public class PlayerAPI {

    private final MongoCollection<Document> players;
    private final Map<UUID, PlayerWrapper> cachedPlayers = new HashMap<>();


    private static PlayerAPI singleton;
    public static PlayerAPI getAPI() {
        if(singleton == null) {
            singleton = new PlayerAPI();
        }

        return singleton;
    }

    private PlayerAPI() {
        MongoDatabase database = Mongo.getDatabase();
        players = database.getCollection("playerData");

        //TODO caches all players on startup: probably not best, but for now it works.
        for (Document doc : players.find()) {
            PlayerWrapper wrapper = PlayerWrapper.fromDocument(doc);
            cachedPlayers.put(wrapper.getUUID(), wrapper);
            System.out.println("Startup Cached " + wrapper.getUUID());
            if (wrapper.guild != null)
                System.out.println("Player is in guild: " + wrapper.guild.getName());

        }
    }

    /**
     *
     * @param uuid
     * @return The PlayerWrapper belonging to the UUID if cached. Null if not cached, use loadWrapper.
     */
    public PlayerWrapper getWrapper(UUID uuid) {
        return cachedPlayers.get(uuid);
    }
    /**
     *
     * @param uuid - UUID of the player to load/create from database.
     * @return The loaded or created PlayerWrapper
     */
    public PlayerWrapper loadWrapper(UUID uuid) {
        PlayerWrapper wrapper = cachedPlayers.get(uuid);
        if(wrapper == null) {
            System.out.println("Uncached player joining: " + uuid);
            Document doc = players.find(eq("uuid", uuid)).first(); //should only be one, so first will have to do.
            if(doc != null) {
                System.out.println("Loaded from database.");
                wrapper = PlayerWrapper.fromDocument(doc);
            }
            else {
                System.out.println("No database entry, created new player: " + uuid);
                wrapper = new PlayerWrapper(uuid);
                players.insertOne(wrapper.serialize());
            }
            cachedPlayers.put(uuid, wrapper);
        }

        return wrapper;
    }

    /**
     * Sets a player's Guild in the database.
     *
     */
    public void setGuild(PlayerWrapper wrapper, Guild guild) {
        wrapper.guild = guild;
        players.findOneAndUpdate(eq("uuid", wrapper.getUUID()), set("guild", guild.getId()));
    }

    public void removeGuild(PlayerWrapper wrapper, Guild guild) {
        wrapper.guild = null;
        players.findOneAndUpdate(eq("uuid", wrapper.getUUID()), set("guild", null));
    }

    public void acceptInvite(PlayerWrapper wrapper, Guild guild) {
        wrapper.invites.remove(guild);
        setGuild(wrapper, guild);
    }

    public void removeInvite(PlayerWrapper wrapper, Guild guild) {
        wrapper.invites.remove(guild);
    }
}
