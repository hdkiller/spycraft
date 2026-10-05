package com.hdkiller.spycraft.director;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hdkiller.spycraft.item.ModItems;
import com.hdkiller.spycraft.item.ParachuteBackpackItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** A disposable film set. Stores identity and coordinates, never entities or worlds. */
public final class ParachuteScene {
    public static final String WORLD_PREFIX = "SPYCRAFT_FILM_";
    private UUID actor;
    private ResourceKey<Level> dimension;
    private int tick = -1;
    private volatile boolean running;
    private JsonArray samples = new JsonArray();
    private boolean deployed;
    private boolean landed;
    private int landingTick = -1;

    public static void requireFilmWorld(MinecraftServer server) {
        if (!server.isSingleplayer() || !server.getWorldData().getLevelName().startsWith(WORLD_PREFIX)) {
            throw new IllegalStateException("Director actions require a dedicated SPYCRAFT_FILM_ singleplayer world");
        }
    }

    public void prepare(MinecraftServer server, UUID actorId) {
        requireFilmWorld(server);
        ServerPlayer player = server.getPlayerList().getPlayer(actorId);
        if (player == null) throw new IllegalStateException("Actor is not connected");
        var level = player.serverLevel();
        if (!level.dimension().equals(Level.OVERWORLD)) throw new IllegalStateException("Use the Overworld film set");
        actor = actorId;
        dimension = level.dimension();
        cancel();
        level.setDayTime(1000);
        level.setWeatherParameters(0, 0, false, false);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        // A garden plaza, glass tower, roof helipad, and marked extraction zone.
        for (int x = -40; x <= 40; x++) {
            for (int z = -20; z <= 85; z++) {
                level.setBlock(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState(), 2);
                boolean border = x == -40 || x == 40 || z == -20 || z == 85;
                level.setBlock(new BlockPos(x, 80, z),
                        (border ? Blocks.SMOOTH_QUARTZ : Blocks.GRASS_BLOCK).defaultBlockState(), 2);
            }
        }
        for (int y = 81; y <= 99; y++) {
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 6; z++) {
                    boolean wall = Math.abs(x) == 7 || z == -7 || z == 6;
                    var block = y == 99 || y % 5 == 0 ? Blocks.GRAY_CONCRETE
                            : wall ? Blocks.LIGHT_BLUE_STAINED_GLASS : Blocks.AIR;
                    level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
                }
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                boolean h = Math.abs(x) == 3 || z == 0;
                level.setBlock(new BlockPos(x, 99, z),
                        (h ? Blocks.WHITE_CONCRETE : Blocks.GRAY_CONCRETE).defaultBlockState(), 2);
            }
        }
        for (int x = -9; x <= 9; x++) {
            for (int z = 20; z <= 78; z++) {
                var block = Math.abs(x) == 9 ? Blocks.YELLOW_CONCRETE : Blocks.GRAY_CONCRETE;
                if (x == 0 && z % 6 < 3) block = Blocks.WHITE_CONCRETE;
                level.setBlock(new BlockPos(x, 80, z), block.defaultBlockState(), 2);
            }
        }
        for (int z : new int[]{16, 32, 48, 64, 80}) {
            for (int x : new int[]{-12, 12}) {
                level.setBlock(new BlockPos(x, 81, z), Blocks.SEA_LANTERN.defaultBlockState(), 2);
                level.setBlock(new BlockPos(x, 82, z), Blocks.OAK_LEAVES.defaultBlockState(), 2);
            }
        }
        resetPlayer(player);
        level.setDefaultSpawnPos(new BlockPos(0, 100, 0), 0);
    }

    private void resetPlayer(ServerPlayer player) {
        ParachuteBackpackItem.closeParachute(player, player.level());
        player.setGameMode(GameType.SURVIVAL);
        player.removeAllEffects();
        player.setHealth(player.getMaxHealth());
        player.getFoodData().setFoodLevel(20);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.PARACHUTE_BACKPACK));
        player.getInventory().setItem(0, new ItemStack(ModItems.GRAPPLING_HOOK_GUN));
        player.getInventory().selected = 0;
        player.teleportTo(player.serverLevel(), 0.5, 100, 0.5, 0, 0);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
    }

    public void start(MinecraftServer server, UUID actorId) {
        requireFilmWorld(server);
        if (!actorId.equals(actor)) throw new IllegalStateException("Prepare the scene first");
        var player = server.getPlayerList().getPlayer(actor);
        if (player == null || !player.level().dimension().equals(dimension)) {
            throw new IllegalStateException("Actor left the film set");
        }
        resetPlayer(player);
        samples = new JsonArray();
        deployed = false;
        landed = false;
        landingTick = -1;
        tick = 0;
        running = true;
    }

    public void tick(MinecraftServer server) {
        if (tick < 0 || tick > 300) return;
        if (!server.isSingleplayer() || !server.getWorldData().getLevelName().startsWith(WORLD_PREFIX)) {
            cancel();
            return;
        }
        var player = server.getPlayerList().getPlayer(actor);
        if (player == null || !player.level().dimension().equals(dimension) || !player.isAlive()) {
            cancel();
            return;
        }
        if (tick == 60) {
            // Step beyond the roof edge, then let the real gadget handle the fall.
            player.teleportTo(player.serverLevel(), 0.5, 100, 7.5, 0, 0);
            player.setDeltaMovement(0, -0.08, 0.1);
            player.fallDistance = 0;
        }
        boolean active = ParachuteBackpackItem.isParachuting(player);
        deployed |= active;
        if (deployed && player.onGround() && !landed) {
            landed = true;
            landingTick = tick;
        }
        if (tick % 5 == 0 || tick == 60 || tick == landingTick) {
            JsonObject sample = new JsonObject();
            sample.addProperty("tick", tick);
            sample.addProperty("x", player.getX());
            sample.addProperty("y", player.getY());
            sample.addProperty("z", player.getZ());
            sample.addProperty("parachuting", active);
            sample.addProperty("onGround", player.onGround());
            samples.add(sample);
        }
        tick++;
        if (tick > 300) running = false;
    }

    public boolean isRunning() { return running; }

    public void cancel() {
        running = false;
        tick = -1;
    }

    public JsonObject status() {
        JsonObject result = new JsonObject();
        result.addProperty("scene", "parachute");
        result.addProperty("tick", tick);
        result.addProperty("complete", tick > 300);
        result.addProperty("deployed", deployed);
        result.addProperty("landed", landed);
        result.addProperty("landingTick", landingTick);
        result.add("samples", samples.deepCopy());
        return result;
    }
}
