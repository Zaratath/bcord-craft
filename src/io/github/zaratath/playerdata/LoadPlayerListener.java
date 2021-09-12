package io.github.zaratath.playerdata;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class LoadPlayerListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void PlayerJoin(AsyncPlayerPreLoginEvent event) {
        PlayerAPI.getAPI().loadWrapper(event.getUniqueId());
    }
}
