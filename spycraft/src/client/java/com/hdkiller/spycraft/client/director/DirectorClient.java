package com.hdkiller.spycraft.client.director;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.hdkiller.spycraft.SpyCraftMod;
import com.hdkiller.spycraft.director.ParachuteScene;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Opt-in local file API for disposable filming worlds; no listener or external access. */
public final class DirectorClient implements ClientModInitializer {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final ParachuteScene scene = new ParachuteScene();
    private Path directory;
    private boolean busy;
    private long nextPoll;
    private int recordingStart;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("spycraft.director")) return;
        directory = Minecraft.getInstance().gameDirectory.toPath().resolve("spycraft-director");
        try {
            Files.createDirectories(directory);
            JsonObject ready = result("ready", "Director enabled");
            ready.addProperty("pid", ProcessHandle.current().pid());
            ready.addProperty("protocolVersion", 1);
            write("ready.json", ready);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize director directory", e);
        }
        ServerTickEvents.END_SERVER_TICK.register(scene::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED
                .register(server -> scene.cancel());
        ClientTickEvents.END_CLIENT_TICK.register(this::poll);
        SpyCraftMod.LOGGER.info("SpyCraft director enabled at {}", directory);
    }

    private void poll(Minecraft client) {
        if (scene.isRunning() && client.player != null) {
            net.minecraft.client.KeyMapping.releaseAll();
            client.mouseHandler.releaseMouse();
            client.player.setYRot(0);
            client.player.setXRot(0);
        }
        if (busy || System.currentTimeMillis() < nextPoll) return;
        nextPoll = System.currentTimeMillis() + 100;
        Path request = directory.resolve("request.json");
        if (!Files.exists(request)) return;
        JsonObject command;
        try {
            command = JSON.fromJson(Files.readString(request), JsonObject.class);
            Files.delete(request);
            String id = command.get("id").getAsString();
            if (!id.matches("[A-Za-z0-9_-]{1,64}")) throw new IllegalArgumentException("Invalid request id");
            busy = true;
            dispatch(client, command, id);
        } catch (Exception e) {
            busy = false;
            SpyCraftMod.LOGGER.error("Director request failed", e);
        }
    }

    private void dispatch(Minecraft client, JsonObject command, String id) {
        try {
            switch (command.get("action").getAsString()) {
                case "create_world" -> {
                    if (client.level != null || client.getSingleplayerServer() != null) {
                        throw new IllegalStateException("Return to the title screen before creating a film world");
                    }
                    String world = ParachuteScene.WORLD_PREFIX + id;
                    if (Files.exists(client.gameDirectory.toPath().resolve("saves").resolve(world))) {
                        throw new IllegalStateException("Film world already exists; use a fresh request id");
                    }
                    GameRules rules = new GameRules();
                    rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                    rules.getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS).set(0, null);
                    var settings = new LevelSettings(world, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                            true, rules, WorldDataConfiguration.DEFAULT);
                    client.createWorldOpenFlows().createFreshLevel(world, settings,
                            new WorldOptions(5762026L, false, false),
                            registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                    .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                            new TitleScreen());
                    finish(id, result("loading", world));
                }
                case "open_world" -> {
                    String name = command.get("world").getAsString();
                    if (!name.matches("SPYCRAFT_FILM_[A-Za-z0-9_-]{1,64}") || client.level != null) {
                        throw new IllegalArgumentException("Open requires a film world name and no loaded world");
                    }
                    client.createWorldOpenFlows().openWorld(name, () -> client.setScreen(new TitleScreen()));
                    finish(id, result("loading", name));
                }
                case "quit" -> {
                    if (client.level != null) throw new IllegalStateException("Disconnect before quitting");
                    finish(id, result("stopping", "Closing the filming client"));
                    client.stop();
                }
                case "prepare", "take", "status" -> {
                    var server = client.getSingleplayerServer();
                    if (server == null || client.player == null) throw new IllegalStateException("No integrated world loaded");
                    ParachuteScene.requireFilmWorld(server);
                    var uuid = client.player.getUUID();
                    String action = command.get("action").getAsString();
                    if (action.equals("take")) recordingStart = ReplayDirector.recordingDuration();
                    server.execute(() -> {
                        JsonObject answer;
                        try {
                            if (action.equals("prepare")) scene.prepare(server, uuid);
                            if (action.equals("take")) scene.start(server, uuid);
                            answer = scene.status();
                            answer.addProperty("recordingStartMs", recordingStart);
                            answer.addProperty("world", server.getWorldData().getLevelName());
                        } catch (Exception e) {
                            answer = result("error", e.toString());
                        }
                        JsonObject response = answer;
                        client.execute(() -> {
                            client.options.pauseOnLostFocus = false;
                            client.setScreen(null);
                            finish(id, response);
                        });
                    });
                }
                case "screenshot" -> {
                    String filename = "director-" + id + ".png";
                    try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
                        image.writeToFile(directory.resolve(filename));
                    }
                    finish(id, result("saved", filename));
                }
                case "disconnect" -> {
                    var server = client.getSingleplayerServer();
                    if (server == null) throw new IllegalStateException("No integrated world loaded");
                    ParachuteScene.requireFilmWorld(server);
                    client.level.disconnect();
                    client.disconnect(new TitleScreen());
                    finish(id, result("disconnected", "ReplayMod is saving the recording"));
                }
                case "export_replay" -> {
                    if (client.getSingleplayerServer() != null) throw new IllegalStateException("Disconnect before exporting");
                    Path replay = Path.of(command.get("replay").getAsString()).toAbsolutePath().normalize();
                    Path recordingRoot = client.gameDirectory.toPath().resolve("replay_recordings").toAbsolutePath().normalize();
                    if (!replay.startsWith(recordingRoot) || !replay.toString().endsWith(".mcpr")) {
                        throw new IllegalArgumentException("Replay must be an .mcpr inside this instance's replay_recordings");
                    }
                    JsonObject take = command.getAsJsonObject("take");
                    Path output = directory.resolve("parachute-" + id + ".mp4");
                    ReplayDirector.export(replay, output, take);
                    finish(id, result("exported", output.toString()));
                }
                default -> throw new IllegalArgumentException("Unknown director action");
            }
        } catch (Throwable e) {
            SpyCraftMod.LOGGER.error("Director action failed", e);
            finish(id, result("error", e.toString()));
        }
    }

    private static JsonObject result(String state, String message) {
        JsonObject result = new JsonObject();
        result.addProperty("state", state);
        result.addProperty("message", message);
        return result;
    }

    private void finish(String id, JsonObject answer) {
        try {
            write("response-" + id + ".json", answer);
        } catch (Exception e) {
            SpyCraftMod.LOGGER.error("Cannot write director response", e);
        } finally {
            busy = false;
        }
    }

    private void write(String filename, JsonObject value) throws Exception {
        Path pending = directory.resolve(filename + ".tmp");
        Files.writeString(pending, JSON.toJson(value));
        Files.move(pending, directory.resolve(filename), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
