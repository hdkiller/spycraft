package com.hdkiller.spycraft.mission;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hdkiller.spycraft.SpyCraftMod;
import com.hdkiller.spycraft.block.LaserPylonBlock;
import com.hdkiller.spycraft.block.ModBlocks;
import com.hdkiller.spycraft.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Pillager;
import net.minecraft.world.entity.monster.Vindicator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;

public class SpyBaseMissionBuilder {
    private static class StructureData {
        int width;
        int height;
        int length;
        String[] palette;
        int[][] blocks; // [x, y, z, palIndex]
    }

    private static StructureData CACHED_STRUCTURE = null;

    private static synchronized StructureData loadStructure() {
        if (CACHED_STRUCTURE != null) return CACHED_STRUCTURE;

        try (InputStream raw = SpyBaseMissionBuilder.class.getResourceAsStream("/data/spycraft/structures/spybase.json.gz")) {
            if (raw == null) {
                SpyCraftMod.LOGGER.error("Could not find /data/spycraft/structures/spybase.json.gz!");
                return null;
            }
            try (GZIPInputStream gz = new GZIPInputStream(raw);
                 InputStreamReader reader = new InputStreamReader(gz, StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                StructureData data = new StructureData();
                data.width = json.get("width").getAsInt();
                data.height = json.get("height").getAsInt();
                data.length = json.get("length").getAsInt();

                JsonArray palArr = json.getAsJsonArray("palette");
                data.palette = new String[palArr.size()];
                for (int i = 0; i < palArr.size(); i++) {
                    data.palette[i] = palArr.get(i).getAsString();
                }

                JsonArray blkArr = json.getAsJsonArray("blocks");
                data.blocks = new int[blkArr.size()][4];
                for (int i = 0; i < blkArr.size(); i++) {
                    JsonArray b = blkArr.get(i).getAsJsonArray();
                    data.blocks[i][0] = b.get(0).getAsInt();
                    data.blocks[i][1] = b.get(1).getAsInt();
                    data.blocks[i][2] = b.get(2).getAsInt();
                    data.blocks[i][3] = b.get(3).getAsInt();
                }

                CACHED_STRUCTURE = data;
                SpyCraftMod.LOGGER.info("Loaded SpyBase structure: {} blocks ({}x{}x{})",
                        data.blocks.length, data.width, data.height, data.length);
                return data;
            }
        } catch (Exception e) {
            SpyCraftMod.LOGGER.error("Failed to load spybase structure", e);
            return null;
        }
    }

    public static boolean deployMission(ServerLevel level, BlockPos origin, Player player) {
        StructureData data = loadStructure();
        if (data == null) return false;

        // Center building around clicked position
        int startX = origin.getX() - (data.width / 2);
        int startY = origin.getY() + 1;
        int startZ = origin.getZ() - (data.length / 2);

        // Resolve palette to BlockStates
        BlockState[] resolvedPalette = new BlockState[data.palette.length];
        for (int i = 0; i < data.palette.length; i++) {
            ResourceLocation id = ResourceLocation.tryParse("minecraft:" + data.palette[i]);
            if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                resolvedPalette[i] = BuiltInRegistries.BLOCK.get(id).defaultBlockState();
            } else {
                resolvedPalette[i] = Blocks.SMOOTH_STONE.defaultBlockState();
            }
        }

        // Place all 67k blocks
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int[] b : data.blocks) {
            mpos.set(startX + b[0], startY + b[1], startZ + b[2]);
            level.setBlock(mpos, resolvedPalette[b[3]], Block.UPDATE_CLIENTS);
        }

        // Deploy SpyCraft Security Features (traps, mobs, boss)
        populateMission(level, startX, startY, startZ, data);

        // Sounds & Announcements
        level.playSound(null, origin, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 2.0f, 1.0f);
        level.playSound(null, origin, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 2.0f, 1.2f);

        if (player != null) {
            player.sendSystemMessage(Component.literal("§6══════════════════════════════════════════════════"));
            player.sendSystemMessage(Component.literal("§b🛰️  [KÉMBÁZIS FELHŐKARCOLÓ SIKERESEN LEHELYEZVE!]"));
            player.sendSystemMessage(Component.literal("§f   Méret: 96 szint | 67.477 blokk | Élesített védelem"));
            player.sendSystemMessage(Component.literal("§e   🎯 KÜLDETÉS CÉLOK:"));
            player.sendSystemMessage(Component.literal("§7    1. Szivárogj be a földszinti lobby-n át az őrök mellett!"));
            player.sendSystemMessage(Component.literal("§7    2. Hatolj át a szerverterem aktív lézercsapdáin!"));
            player.sendSystemMessage(Component.literal("§7    3. Juss fel a 80. emeleti Penthouse-ba és iktasd ki a Főgonoszt!"));
            player.sendSystemMessage(Component.literal("§6══════════════════════════════════════════════════"));
        }

        return true;
    }

    private static void populateMission(ServerLevel level, int startX, int startY, int startZ, StructureData data) {
        int centerX = startX + (data.width / 2);
        int centerZ = startZ + (data.length / 2);

        // 1. Ground Floor Lobby Guards (Y+2)
        spawnGuard(level, centerX - 5, startY + 2, centerZ, "Biztonsági Őr (Lobby)");
        spawnGuard(level, centerX + 5, startY + 2, centerZ, "Biztonsági Őr (Lobby)");
        spawnGuard(level, centerX, startY + 2, centerZ - 8, "Recepciós Őr");
        spawnGuard(level, centerX, startY + 2, centerZ + 8, "Kapu Őr");

        // 2. Mid-level Laser Defense System (Y+21)
        BlockPos l1 = new BlockPos(centerX - 4, startY + 21, centerZ - 4);
        BlockPos l2 = new BlockPos(centerX + 4, startY + 21, centerZ - 4);
        BlockPos l3 = new BlockPos(centerX + 4, startY + 21, centerZ + 4);
        BlockPos l4 = new BlockPos(centerX - 4, startY + 21, centerZ + 4);

        level.setBlock(l1, ModBlocks.LASER_PYLON.defaultBlockState().setValue(LaserPylonBlock.ACTIVE, true), Block.UPDATE_ALL);
        level.setBlock(l2, ModBlocks.LASER_PYLON.defaultBlockState().setValue(LaserPylonBlock.ACTIVE, true), Block.UPDATE_ALL);
        level.setBlock(l3, ModBlocks.LASER_PYLON.defaultBlockState().setValue(LaserPylonBlock.ACTIVE, true), Block.UPDATE_ALL);
        level.setBlock(l4, ModBlocks.LASER_PYLON.defaultBlockState().setValue(LaserPylonBlock.ACTIVE, true), Block.UPDATE_ALL);

        // Sound trap decoy nearby
        level.setBlock(new BlockPos(centerX, startY + 21, centerZ), ModBlocks.SOUND_TRAP.defaultBlockState(), Block.UPDATE_ALL);

        // Mid-floor Elite Tech Agents (Y+21)
        spawnEliteAgent(level, centerX - 3, startY + 21, centerZ + 3, "Elite Hálózatőr");
        spawnEliteAgent(level, centerX + 3, startY + 21, centerZ - 3, "Elite Hálózatőr");

        // 3. Top Floor Penthouse (Y+81) - Mastermind Boss
        spawnMastermindBoss(level, centerX, startY + 81, centerZ);

        // 4. Secret Loot Vault Chest (Y+81)
        BlockPos chestPos = new BlockPos(centerX + 3, startY + 81, centerZ + 3);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            chest.setItem(0, new ItemStack(Items.DIAMOND, 12));
            chest.setItem(1, new ItemStack(Items.EMERALD, 32));
            chest.setItem(2, new ItemStack(ModItems.SPY_GOGGLES));
            chest.setItem(3, new ItemStack(ModBlocks.C4_BLOCK, 4));
            chest.setItem(4, new ItemStack(ModItems.REMOTE_DETONATOR));
            chest.setItem(5, new ItemStack(ModItems.SNIPER_RIFLE));
        }
    }

    private static void spawnGuard(ServerLevel level, int x, int y, int z, String name) {
        Pillager guard = EntityType.PILLAGER.create(level);
        if (guard != null) {
            guard.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            guard.setCustomName(Component.literal("§c" + name));
            guard.setCustomNameVisible(true);
            guard.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
            guard.setPersistenceRequired();
            level.addFreshEntity(guard);
        }
    }

    private static void spawnEliteAgent(ServerLevel level, int x, int y, int z, String name) {
        Vindicator agent = EntityType.VINDICATOR.create(level);
        if (agent != null) {
            agent.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            agent.setCustomName(Component.literal("§4" + name));
            agent.setCustomNameVisible(true);
            agent.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
            agent.setPersistenceRequired();
            level.addFreshEntity(agent);
        }
    }

    private static void spawnMastermindBoss(ServerLevel level, int x, int y, int z) {
        Vindicator boss = EntityType.VINDICATOR.create(level);
        if (boss != null) {
            boss.moveTo(x + 0.5, y, z + 0.5, 0, 0);
            boss.setCustomName(Component.literal("§4☠️ Főgonosz Mesterelme (Mastermind Boss)"));
            boss.setCustomNameVisible(true);
            boss.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
            boss.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
            boss.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_AXE));
            boss.setHealth(60.0f); // High boss health
            boss.setPersistenceRequired();
            level.addFreshEntity(boss);
        }
    }
}
