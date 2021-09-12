package io.github.zaratath.reinforce;

import com.mongodb.client.MongoCollection;
import de.jeff_media.customblockdata.CustomBlockData;
import io.github.zaratath.BcordCraft;
import io.github.zaratath.database.Mongo;
import io.github.zaratath.playerdata.guilds.Guild;
import io.github.zaratath.playerdata.guilds.GuildAPI;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class Reinforcement {
    public static final NamespacedKey REINFORCEMENT_KEY = new NamespacedKey(BcordCraft.getInstance(), "reinforcement");
    public static final MongoCollection reinfCollection = Mongo.getDatabase().getCollection("reinforcements");
    /**
     *
     * @param block
     * @return Reinforcement of a block, null if none.
     */
    public static Reinforcement getReinforcement(Block block) {
        final PersistentDataContainer customBlockData = new CustomBlockData(block, BcordCraft.getInstance());
        if (!customBlockData.has(REINFORCEMENT_KEY, PersistentDataType.STRING))
            return null;
        String str = customBlockData.get(REINFORCEMENT_KEY, PersistentDataType.STRING);
        Reinforcement reinf = Reinforcement.fromString(str);
        //guild no longer exists for some reason.
        if(reinf.guild == null) {
            removeReinforcement(block);
            return null;
        }
        return reinf;
    }

    /**
     *  Attempts to add a new reinforcement to a block.
     *
     * @param block
     * @param reinf
     * @return True if successful, false if not.
     */
    public static boolean newReinforcement(Block block, Reinforcement reinf) {
        final PersistentDataContainer customBlockData = new CustomBlockData(block, BcordCraft.getInstance());
        if (customBlockData.has(REINFORCEMENT_KEY, PersistentDataType.STRING))
            return false;
        updateReinforcement(block, reinf);
        return true;
    }

    public static void updateReinforcement(Block block, Reinforcement reinf) {
        if(reinf.durability <= 0) {
            removeReinforcement(block);
            return;
        }
        final PersistentDataContainer customBlockData = new CustomBlockData(block, BcordCraft.getInstance());
        String str = reinf.toString();
        customBlockData.set(REINFORCEMENT_KEY, PersistentDataType.STRING, str);

    }

    public static void removeReinforcement(Block block) {
        final PersistentDataContainer customBlockData = new CustomBlockData(block, BcordCraft.getInstance());
        customBlockData.remove(REINFORCEMENT_KEY);
    }


    public final ReinforcementType type;
    public final Guild guild;
    public int durability;

    public Reinforcement(ReinforcementType type, Guild guild, int durability) {
        this.type = type;
        this.guild = guild;
        this.durability = durability;
    }

    public static Reinforcement fromString(String str) {
        String[] strings = str.split(";");
        return new Reinforcement(
                ReinforcementType.valueOf(strings[0]),
                GuildAPI.getAPI().getGuild(Integer.valueOf(strings[1])),
                Integer.valueOf(strings[2])
        );
    }

    @Override
    public String toString() {
        String str = String.join(";", this.type.toString(), String.valueOf(this.guild.getId()), String.valueOf(this.durability));
        return str;
    }

}
