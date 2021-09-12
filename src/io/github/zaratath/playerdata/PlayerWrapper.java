package io.github.zaratath.playerdata;

import io.github.zaratath.database.DocumentSerializable;
import io.github.zaratath.playerdata.guilds.Guild;
import io.github.zaratath.playerdata.guilds.GuildAPI;
import org.bson.Document;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Wraps the Player object
 *  */
public class PlayerWrapper implements DocumentSerializable {

    private final UUID uuid;
    public Guild guild;
    public Set<Guild> invites = new HashSet<>();

    /**
     * New empty playerwrapper.
     * @param uuid
     */
    public PlayerWrapper(UUID uuid) {
        this.uuid = uuid;
    }

    public static PlayerWrapper fromDocument(Document document) {

        UUID uuid = document.get("uuid", UUID.class);
        Integer guildId = document.getInteger("guild");
        Guild guild = (guildId == null) ? null : GuildAPI.getAPI().getGuild(guildId);
        Set<Guild> invites = document.getList("invites", Integer.class)
                .stream()
                .map(GuildAPI.getAPI()::getGuild)
                .collect(Collectors.toSet());

        PlayerWrapper player = new PlayerWrapper(uuid);
        player.guild = guild;
        player.invites = invites;

        return player;
    }

    public Document serialize() {
        Document document = new Document();
        document.put("uuid", this.uuid);
        if(this.guild != null) {
            document.put("guild", this.guild.getId());
        }
        document.put("invites", invites.stream().map(g -> {
            return g.getId();
        }).collect(Collectors.toList()));

        return document;
    }

    public UUID getUUID() {
        return this.uuid;
    }

}
