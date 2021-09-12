package io.github.zaratath.playerdata.guilds;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.CustomArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import io.github.zaratath.BcordCraft;
import io.github.zaratath.playerdata.PlayerAPI;
import io.github.zaratath.playerdata.PlayerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class GuildCommands {

    private static Set<Guild> queriedDeletion = new HashSet();

    public static Predicate<CommandSender> playerIsNotInGuild = sender -> {
        return PlayerAPI.getAPI().getWrapper(((Player) sender).getUniqueId()).guild == null;
    };
    public static Predicate<CommandSender> playerIsInGuild = sender -> {
        return PlayerAPI.getAPI().getWrapper(((Player) sender).getUniqueId()).guild != null;
    };
    public static Predicate<CommandSender> playerIsOwnerOfGuild = sender -> {
        Player player = (Player) sender;
        Guild guild = PlayerAPI.getAPI().getWrapper(player.getUniqueId()).guild;

        if(guild == null) {
            return false;
        }
        return guild.owner.equals(player.getUniqueId());
    };
    public static Predicate<CommandSender> playerIsInvitedToGuild = sender -> {
        return PlayerAPI.getAPI().getWrapper(((Player) sender).getUniqueId()).invites.size() > 0;
    };
    public static Predicate<CommandSender> guildIsQueriedForDelete = sender -> {
        return queriedDeletion.contains(PlayerAPI.getAPI().getWrapper(((Player) sender).getUniqueId()).guild);
    };
    public static void register() {
        new CommandAPICommand("guild")
                .withSubcommand(new CommandAPICommand("create")
                        .withRequirement(playerIsNotInGuild)
                        .withArguments(new GreedyStringArgument("name"))
                        .executesPlayer((sender, args) -> {
                            String name = (String) args[0];
                            Guild guild = GuildAPI.getAPI().createGuild(sender.getUniqueId(),name);
                            if(guild == null) {
                                sender.sendMessage("A guild exists by that name already.");
                                return;
                            }
                            sender.sendMessage("Created guild \""+guild.getName()+"\"");
                            PlayerWrapper wrapper = PlayerAPI.getAPI().getWrapper(sender.getUniqueId());
                            PlayerAPI.getAPI().setGuild(wrapper, guild);
                            CommandAPI.updateRequirements(sender); //cuz player is now owner of a guild
                        }))
                .withSubcommand(new CommandAPICommand("delete")
                        .withRequirement(playerIsOwnerOfGuild)
                        .executesPlayer((sender, args) -> {
                            sender.sendMessage("Are you sure you want to delete your guild?");
                            sender.sendMessage("Use "+ ChatColor.RED + "\"/guild delete confirm\""+ChatColor.RESET+" if you really want to delete your guild.");
                            Guild guild = PlayerAPI.getAPI().getWrapper(((Player) sender).getUniqueId()).guild;
                            queriedDeletion.add(guild);
                            CommandAPI.updateRequirements(sender);
                            Bukkit.getScheduler().runTaskLater(BcordCraft.getInstance(), () -> {
                                        if(queriedDeletion.remove(guild)) {
                                            sender.sendMessage("Your request to delete your guild has timed out.");
                                        };
                                        CommandAPI.updateRequirements(sender);
                                    }, 20*30);
                        })
                        .withSubcommand(new CommandAPICommand("confirm")
                                .withRequirement(playerIsOwnerOfGuild)
                                .withRequirement(guildIsQueriedForDelete)
                                .executesPlayer((sender, args) -> {
                                    Guild guild = PlayerAPI.getAPI().getWrapper(sender.getUniqueId()).guild;
                                    queriedDeletion.remove(guild);
                                    GuildAPI.getAPI().deleteGuild(guild);
                                }))
                )
                .withSubcommand(new CommandAPICommand("rename")
                        .withRequirement(playerIsOwnerOfGuild)
                        .withArguments(new GreedyStringArgument("new name"))
                        .executesPlayer((sender, args) -> {
                            String name = (String) args[0];
                            Guild guild = PlayerAPI.getAPI().getWrapper(sender.getUniqueId()).guild;
                            String oldName = guild.getName();
                            GuildAPI.getAPI().renameGuild(guild, name);
                            sender.sendMessage(
                                    String.format("Your guild %s\"%s\"%s has been renamed to %s\"%s\"",
                                    ChatColor.RED, oldName, ChatColor.RESET, ChatColor.RED, name)
                            );
                        }))
                .withSubcommand(new CommandAPICommand("invite")
                        .withRequirement(playerIsOwnerOfGuild)
                        .withArguments(new PlayerArgument("player")
                                .replaceWithSafeSuggestions((info) -> {
                                    return Bukkit.getOnlinePlayers()
                                            .stream()
                                            .filter((player) -> {
                                                PlayerAPI api = PlayerAPI.getAPI();
                                                PlayerWrapper invitee = api.getWrapper(player.getUniqueId());
                                                PlayerWrapper inviter = api.getWrapper(((Player) info.sender()).getUniqueId());
                                                Guild guild = inviter.guild;
                                                return (!guild.getMembers().contains(invitee.getUUID()) && !invitee.invites.contains(inviter.guild));
                                            })
                                            .toArray(Player[]::new);

                                }))
                        .executesPlayer((sender, args) -> {
                            Player target = (Player) args[0];
                            PlayerAPI playerApi = PlayerAPI.getAPI();

                            //shouldn't be null because of the predicate requiring the sender to be an owner.
                            Guild guild = playerApi.getWrapper(sender.getUniqueId()).guild;

                            if(guild.getMembers().contains(target.getUniqueId())) {
                                sender.sendMessage("That player is already in your guild.");
                                return;
                            }
                            if(!playerApi.getWrapper(target.getUniqueId()).invites.contains(guild)) {
                                sender.sendMessage("That player has already been invited to your guild.");
                                return;
                            }
                            GuildAPI.getAPI().invitePlayer(target.getUniqueId(), guild);
                            CommandAPI.updateRequirements(target); //updates player to access the /accept command.
                        }))
                .withSubcommand(new CommandAPICommand("accept")
                        .withRequirement(playerIsInvitedToGuild)
                        .withArguments(new CustomArgument<Guild>("guild", (input) -> {
                            return GuildAPI.getAPI().getGuild(input.input());
                        }).replaceSuggestions((info) ->
                            PlayerAPI.getAPI().getWrapper(((Player) info.sender()).getUniqueId()).invites
                                    .stream()
                                    .map((g) -> {
                                        return "\"" + g.getName() + "\"";
                                    })
                                    .toArray(String[]::new)
                        ))
                        .executesPlayer((sender, args) -> {
                            PlayerWrapper wrapper = PlayerAPI.getAPI().getWrapper(sender.getUniqueId());
                            Guild guild = (Guild) args[0];
                            if(!wrapper.invites.contains(guild)) {
                                sender.sendMessage("You are not invited to " + guild.getName());
                                return;
                            }
                            ////
                            GuildAPI.getAPI().acceptInvite(wrapper, guild);
                            CommandAPI.updateRequirements(sender); //player is no longer invited.
                            ////
                        })
                )
                .withSubcommand(new CommandAPICommand("members")
                        .withRequirement(playerIsInGuild)
                        .executesPlayer((sender, args) -> {
                            Guild guild = PlayerAPI.getAPI().getWrapper(sender.getUniqueId()).guild;
                            sender.sendMessage(guild.getMembers().toString());
                        })
                )
                //Lists all Guilds in the server.
                .withSubcommand(new CommandAPICommand("list")
                        .executes((sender, args) -> {
                            sender.sendMessage(GuildAPI.getAPI().getGuildNames().toString());
                        }))
                .register();
    }
}
