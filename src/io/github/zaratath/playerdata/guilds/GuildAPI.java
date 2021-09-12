package io.github.zaratath.playerdata.guilds;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.zaratath.database.Mongo;
import io.github.zaratath.playerdata.PlayerAPI;
import io.github.zaratath.playerdata.PlayerWrapper;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;

public class GuildAPI {

    private final MongoCollection<Document> guilds;

    private Map<Integer, Guild> guildsById = new HashMap<>();
    private Map<String, Guild> guildsByName = new HashMap<>();

    private int currentIdTally = 0;

    private static GuildAPI singleton;
    public static GuildAPI getAPI() {
       if(singleton == null) {
           singleton = new GuildAPI();
       }

       return singleton;
    }

    private GuildAPI() {
        MongoDatabase database = Mongo.getDatabase();
        guilds = database.getCollection("guilds");

        //caches all guilds on startup: probably not best, but for now it works.
        for (Document doc : guilds.find()) {
            Guild guild =  Guild.fromDocument(doc);
            int guildId = doc.getInteger("id");
            if(guildId > currentIdTally) {
                currentIdTally = guildId;
            }
            System.out.println("Cached guild: " + guild.getName());
            cacheGuild(guild);
        }
    }

    private void cacheGuild(Guild guild) {
        guildsById.put(guild.getId(), guild);
        guildsByName.put(guild.getName(), guild);
    }

    /**
     * Tries to find the guild, first in cache then in the database. Null if not found.
     * Guild names can change arbitrarily and shouldn't be used for storage.
     * @param name
     */
    public Guild getGuild(String name) {
        Guild guild = guildsByName.get(name);

        if(guild == null) {
            //shouldn't be null if cacheing on start is working properly.
            Document doc = guilds.find(eq("name", name)).first(); //should only be one, so first will have to do.
            if(doc != null) {
                guild = Guild.fromDocument(doc);
                cacheGuild(guild);
            }
        }

        return guild;
    }

    public Guild getGuild(int id) {
        Guild guild = guildsById.get(id);

        if(guild == null) {
            //shouldn't be null if cacheing on start is working properly.
            Document doc = guilds.find(eq("id", id)).first(); //should only be one, so first will have to do.
            if(doc != null) {
                guild = Guild.fromDocument(doc);
                cacheGuild(guild);
            }
        }

        return guild;
    }

    public void deleteGuild(Guild guild) {
        PlayerAPI api = PlayerAPI.getAPI();
        for(UUID uuid: guild.members) {
            api.removeGuild(api.getWrapper(uuid), guild);
        }

        for(UUID uuid: guild.invites) {
            api.removeInvite(api.getWrapper(uuid), guild);
        }


        guildsByName.remove(guild.getName());
        guildsById.remove(guild.getId());
        guilds.deleteOne(eq("id", guild.getId()));
    }

    public Set<String> getGuildNames() {
        return guildsByName.keySet();
    }

    /**
     *
     * @param owner
     * @param name
     * @return The new guild, or null if it already existed.
     */
    public Guild createGuild(UUID owner, String name) {
        if(getGuild(name) != null) {
            return null;
        }

        Guild guild = new Guild(owner, name, currentIdTally++);
        cacheGuild(guild);
        guilds.insertOne(guild.serialize());

        return guild;
    }

    public void renameGuild(Guild guild, String name) {
        guildsByName.remove(guild.getName());
        guild.setName(name);
        guildsByName.put(name, guild);
        guilds.findOneAndUpdate(eq("id", guild.getId()), set("name", name));
    }

    /**
     * Invites the given player to the given guild.
     * @param uuid - Player to invite.
     * @param guild - Guild to invite to.
     */
    public void invitePlayer(UUID uuid, Guild guild) {
        PlayerAPI.getAPI().getWrapper(uuid).invites.add(guild);
        guild.addInvite(uuid);
        guilds.findOneAndUpdate(
                eq("id", guild.getId()),
                set("invites", guild.invites)
        );
        Player player = Bukkit.getPlayer(uuid);
        player.sendMessage("You've been invited to join " + guild.getName());
        player.sendMessage(ChatColor.RED + "/guild accept \"" + guild.getName() + "\""+ChatColor.RESET + " to accept.");
    }

    public void acceptInvite(PlayerWrapper wrapper, Guild guild) {
        PlayerAPI.getAPI().acceptInvite(wrapper, guild);
        guild.acceptInvite(wrapper.getUUID());
        guilds.findOneAndUpdate(
                eq("id", guild.getId()),
                combine(set("invites", guild.invites), set("members", guild.members))
        );
    }


}
