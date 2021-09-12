package io.github.zaratath.playerdata.guilds;

import io.github.zaratath.database.DocumentSerializable;
import org.bson.Document;

import java.util.*;

public class Guild implements DocumentSerializable {

    public UUID owner;
    private String name;
    private int id;
    //TODO remove owner field, replace with configurable guildranks.

    public Set<UUID> members;
    public Set<UUID> invites;
    //TODO action log in the future for any administrative actions?
    //player invited, joined, left, kicked. by whom. ranks changed etc

    public Guild(UUID owner, String name, int id) {
        this.owner = owner;
        this.name = name;
        this.id = id;
        this.members = new HashSet<>();
        this.members.add(owner);
        this.invites = new HashSet<>();
    }

    public static Guild fromDocument(Document document) {
        UUID owner = document.get("owner", UUID.class);
        String name = document.getString("name");
        int id = document.getInteger("id");
        Guild guild = new Guild(owner, name, id);

        //TODO tidy up serialization/deserialization
        List<UUID> members = document.getList("members", UUID.class);
        guild.members.addAll(members);
        List<UUID> invites = document.getList("invites", UUID.class);
        guild.invites.addAll(invites);

        return guild;
    }

    public Document serialize() {
        Document document = new Document();
        document.put("owner", owner);
        document.put("name", name);
        document.put("id", id);
        document.put("members", members);
        document.put("invites", invites);
        return document;
    }

    public void addInvite(UUID uuid) {
        invites.add(uuid);
    }

    public void removeInvite(UUID uuid) {
        invites.remove(uuid);
    }

    public void acceptInvite(UUID uuid) {
        invites.remove(uuid);
        members.add(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }
    
    public Set<UUID> getMembers() {
        return this.members;
    }
}
