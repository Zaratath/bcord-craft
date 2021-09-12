package io.github.zaratath.admin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class InventoryViewer implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) return false;
        if(args.length < 1) return false;

        Player player = (Player) sender;
        Player target = Bukkit.getPlayer(args[0]);

        if(target == null) return false;

        TextComponent title = Component.text(
                "\uF801" + ChatColor.WHITE + "\uE000" + //image
                        "\uF80C\uF80A\uf802" + ChatColor.BLACK+ target.getName() + "'s Inventory"); //text and negspace

        Inventory inv = Bukkit.createInventory(null, 54, title);
        player.openInventory(inv);
        return true;
    }
}
