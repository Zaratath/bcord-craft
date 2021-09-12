package io.github.zaratath.reinforce;

import org.bukkit.Material;

public enum ReinforcementType {
    STONE(Material.STONE, 25),
    IRON(Material.IRON_INGOT, 200),
    DIAMOND(Material.DIAMOND, 500);

    private final Material material;
    private final int durability;
    ReinforcementType(Material material, int durability) {
        this.material = material;
        this.durability = durability;
    }

    public int getDurability() {
        return this.durability;
    }

    public Material getMaterial() {
        return this.material;
    }

    public static ReinforcementType getType(Material material) {
        switch(material) {
            case STONE:
                return ReinforcementType.STONE;
            case IRON_INGOT:
                return ReinforcementType.IRON;
            case DIAMOND:
                return ReinforcementType.DIAMOND;
        }
        return null;
    }
}
