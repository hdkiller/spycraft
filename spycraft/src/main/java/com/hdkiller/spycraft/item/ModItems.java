package com.hdkiller.spycraft.item;

import com.hdkiller.spycraft.SpyCraftMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;

public class ModItems {
    // Erik's Star - Right click to strike lightning!
    public static final Item ERIKS_STAR = registerItem(
        "eriks_star",
        new EriksStarItem(new Item.Properties().rarity(Rarity.EPIC))
    );

    // Erik's Lightning Sword - An ultra-powerful diamond-tier sword!
    public static final Item ERIKS_SWORD = registerItem(
        "eriks_sword",
        new SwordItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.RARE).attributes(
            SwordItem.createAttributes(Tiers.NETHERITE, 7, -2.2f)
        ))
    );

    // Tactical Sniper Rifle - Long-range zoom scope & supersonic bullet
    public static final Item SNIPER_RIFLE = registerItem(
        "sniper_rifle",
        new SniperRifleItem(new Item.Properties().rarity(Rarity.EPIC).durability(600))
    );

    // Grappling Hook Gun - Tactical spy infiltration tool
    public static final Item GRAPPLING_HOOK_GUN = registerItem(
        "grappling_hook_gun",
        new GrapplingHookGunItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1))
    );

    // Spy Bug & Radar Tracker - Mount on mobs or blocks, ping radar
    public static final Item SPY_TRACKER = registerItem(
        "spy_tracker",
        new MobTrackerItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1))
    );

    // Tactical Spy Goggles - Night Vision & sky-high beacon pillar
    public static final Item SPY_GOGGLES = registerItem(
        "spy_goggles",
        new SpyGogglesItem(new Item.Properties().rarity(Rarity.EPIC).durability(450))
    );

    // GPS Waypoint Navigator Visor - 3D floating guidance arrows in the air
    public static final Item GPS_NAVIGATOR_GOGGLES = registerItem(
        "gps_navigator_goggles",
        new GpsNavigatorGogglesItem(new Item.Properties().rarity(Rarity.EPIC).durability(450))
    );

    // Villager Spy Disguise Robe - Morphs player into a real Villager!
    public static final Item VILLAGER_DISGUISE_ROBE = registerItem(
        "villager_disguise_robe",
        new VillagerDisguiseItem(ArmorItem.Type.CHESTPLATE, new Item.Properties().rarity(Rarity.EPIC).durability(350))
    );

    // Villager Spy Disguise Mask - Big nose & unibrow mask
    public static final Item VILLAGER_DISGUISE_MASK = registerItem(
        "villager_disguise_mask",
        new VillagerDisguiseItem(ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC).durability(350))
    );

    // Remote Detonator - Syncs to placed C4 and detonates on command (3 yield levels)
    public static final Item REMOTE_DETONATOR = registerItem(
        "remote_detonator",
        new RemoteDetonatorItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1))
    );

    // Laser Forcefield Remote - Links pylons and toggles impassable laser wall
    public static final Item LASER_REMOTE = registerItem(
        "laser_remote",
        new LaserRemoteItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1))
    );

    // Recon Drone - Launchable flying recon drone with laser tag & kamikaze
    public static final Item RECON_DRONE = registerItem(
        "recon_drone",
        new ReconDroneItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1))
    );

    // Mission Beacon - Deploys 96-story Spy Base Skyscraper with guards & defenses
    public static final Item MISSION_BEACON = registerItem(
        "mission_beacon",
        new MissionBeaconItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1))
    );

    // Thermal Vision Goggles - Heat detection through solid walls
    public static final Item THERMAL_GOGGLES = registerItem(
        "thermal_goggles",
        new ThermalGogglesItem(new Item.Properties().rarity(Rarity.EPIC).durability(500))
    );

    // Hologram Decoy Projector - Spawns aggro-taunting duplicate of the player
    public static final Item HOLOGRAM_PROJECTOR = registerItem(
        "hologram_projector",
        new HologramProjectorItem(new Item.Properties().rarity(Rarity.EPIC).stacksTo(1))
    );

    // Tranquilizer Dart Gun - Ultra quiet sleep/freeze dart gun
    public static final Item TRANQUILIZER_GUN = registerItem(
        "tranquilizer_gun",
        new TranquilizerGunItem(new Item.Properties().rarity(Rarity.RARE).durability(250))
    );

    // Tactical Smoke Grenade - Throwable 11m smoke screen for escape
    public static final Item SMOKE_GRENADE = registerItem(
        "smoke_grenade",
        new SmokeGrenadeItem(new Item.Properties().rarity(Rarity.UNCOMMON).stacksTo(16))
    );

    // Magnetic Climbing Gloves - Scale walls and hang in mid-air
    public static final Item CLIMBING_GLOVES = registerItem(
        "climbing_gloves",
        new ClimbingGlovesItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1))
    );

    // Tactical Parachute Backpack - Auto-deploy on fall when worn, manual deploy from hand
    public static final Item PARACHUTE_BACKPACK = registerItem(
        "parachute_backpack",
        new ParachuteBackpackItem(new Item.Properties().rarity(Rarity.EPIC).durability(450))
    );

    // Tactical Recon Binoculars - Optical zoom with dwell lock-on enemy tagging
    public static final Item BINOCULARS = registerItem(
        "binoculars",
        new BinocularsItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1))
    );

    // Tactical Zipline Gun - Wire rope cable launcher and high-speed trolley pulley
    public static final Item ZIPLINE_GUN = registerItem(
        "zipline_gun",
        new ZiplineGunItem(new Item.Properties().rarity(Rarity.RARE).durability(300))
    );

    private static Item registerItem(String name, Item item) {
        return Registry.register(
            BuiltInRegistries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SpyCraftMod.MOD_ID, name),
            item
        );
    }

    public static void registerModItems() {
        SpyCraftMod.LOGGER.info("Registering custom items for " + SpyCraftMod.MOD_ID);
    }
}
