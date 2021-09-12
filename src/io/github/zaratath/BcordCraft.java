package io.github.zaratath;

import io.github.zaratath.admin.TestListener;
import io.github.zaratath.database.Mongo;
import io.github.zaratath.enchantments.EnchantmentListener;
import io.github.zaratath.playerdata.LoadPlayerListener;
import io.github.zaratath.playerdata.PlayerAPI;
import io.github.zaratath.playerdata.guilds.GuildAPI;
import io.github.zaratath.playerdata.guilds.GuildCommands;
import io.github.zaratath.qol.BundleListener;
import io.github.zaratath.qol.CauldronListener;
import io.github.zaratath.reinforce.listeners.ReinforceListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class BcordCraft extends JavaPlugin {
    private static JavaPlugin instance;
    public static JavaPlugin getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        Mongo.getDatabase(); //to instantiate the DB singleton.
        GuildAPI.getAPI(); //to instantiate the guild API singleton.
        PlayerAPI.getAPI(); //instantiates the playerapi singleton.
        GuildCommands.register(); //registers guild commands.
    }

    @Override
    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new TestListener(), this);
        pm.registerEvents(new LoadPlayerListener(), this);
        pm.registerEvents(new EnchantmentListener(), this);

        //reinforcements
        //TODO fix reinforcements and re-enable.
        pm.registerEvents(new ReinforceListener(), this);

        //qol
        pm.registerEvents(new BundleListener(), this);
        pm.registerEvents(new CauldronListener(), this);
        // pm.registerEvents(new ShulkerListener(), this); this is broken as frick for now
    }

    @Override
    public void onDisable() {

    }

}
