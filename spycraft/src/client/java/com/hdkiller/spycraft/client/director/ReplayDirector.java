package com.hdkiller.spycraft.client.director;

import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Map;

/** Optional ReplayMod integration; no compile dependency in the normal mod. */
final class ReplayDirector {
    private ReplayDirector() {}
    static int recordingDuration() throws ReflectiveOperationException {
        Object recording = singleton("com.replaymod.recording.ReplayModRecording");
        Object connection = call(recording, "getConnectionEventHandler");
        Object listener = call(connection, "getPacketListener");
        if (listener == null) throw new IllegalStateException("ReplayMod is not recording this world");
        int duration = ((Number) call(listener, "getCurrentDuration")).intValue();
        listener.getClass().getMethod("addMarker", String.class).invoke(listener, "SpyCraft parachute take");
        return duration;
    }
    static void export(Path replay, Path output, JsonObject take) throws Throwable {
        if (!take.get("complete").getAsBoolean() || !take.get("deployed").getAsBoolean()
                || !take.get("landed").getAsBoolean()) {
            throw new IllegalArgumentException("Export requires a completed take with deployment and landing");
        }
        Object module = singleton("com.replaymod.replay.ReplayModReplay");
        Object handler = null;
        Minecraft client = Minecraft.getInstance();
        boolean oldHideGui = client.options.hideGui;
        int oldFov = client.options.fov().get();
        try {
            Object core = singleton("com.replaymod.core.ReplayMod");
            Object files = core.getClass().getField("files").get(core);
            Object opened = files.getClass().getMethod("open", Path.class).invoke(files, replay);
            handler = module.getClass().getMethod("startReplay",
                    Class.forName("com.replaymod.replaystudio.replay.ReplayFile"), boolean.class, boolean.class)
                    .invoke(module, opened, true, false);
            if (handler == null) throw new IllegalStateException("Replay did not open");
            Class<?> spClass = Class.forName("com.replaymod.simplepathing.SPTimeline");
            Object sp = spClass.getConstructor().newInstance();
            Class<?> interpolator = Class.forName("com.replaymod.simplepathing.InterpolatorType");
            spClass.getMethod("setDefaultInterpolatorType", interpolator)
                    .invoke(sp, interpolator.getField("LINEAR").get(null));
            int start = take.get("recordingStartMs").getAsInt();
            spClass.getMethod("addTimeKeyframe", long.class, int.class).invoke(sp, 0L, start);
            spClass.getMethod("addTimeKeyframe", long.class, int.class).invoke(sp, 15000L, start + 15000);
            var addPosition = spClass.getMethod("addPositionKeyframe", long.class,
                    double.class, double.class, double.class, float.class, float.class, float.class, int.class);
            for (var entry : take.getAsJsonArray("samples")) {
                JsonObject s = entry.getAsJsonObject();
                long time = s.get("tick").getAsLong() * 50;
                double x = s.get("x").getAsDouble(), y = s.get("y").getAsDouble(), z = s.get("z").getAsDouble();
                double opening = Math.max(0, 1 - time / 3000.0);
                double cx = x + 10 + opening * 9, cy = y + 4 + opening * 6, cz = z - 9 - opening * 7;
                double dx = x - cx, dy = y + 1.4 - cy, dz = z - cz;
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
                addPosition.invoke(sp, time, cx, cy, cz, yaw, pitch, 0f, -1);
            }
            Object timeline = call(sp, "getTimeline");
            Class<?> timelineClass = Class.forName("com.replaymod.replaystudio.pathing.path.Timeline");
            Class<?> registryClass = Class.forName("com.replaymod.replaystudio.pathing.PathingRegistry");
            Object replayFile = call(handler, "getReplayFile");
            Class.forName("com.replaymod.replaystudio.replay.ReplayFile")
                    .getMethod("writeTimelines", registryClass, Map.class)
                    .invoke(replayFile, sp, Map.of("SpyCraft parachute pilot", timeline));
            Class.forName("com.replaymod.replaystudio.replay.ReplayFile").getMethod("save").invoke(replayFile);
            Object pathing = singleton("com.replaymod.simplepathing.ReplayModSimplePathing");
            pathing.getClass().getMethod("setCurrentTimeline", spClass).invoke(pathing, sp);
            Minecraft.getInstance().options.hideGui = true;
            Minecraft.getInstance().options.fov().set(60);
            Object method = constant("com.replaymod.render.RenderSettings$RenderMethod", "DEFAULT");
            Object encoding = constant("com.replaymod.render.RenderSettings$EncodingPreset", "MP4_CUSTOM");
            Object aa = constant("com.replaymod.render.RenderSettings$AntiAliasing", "NONE");
            Class<?> settingsClass = Class.forName("com.replaymod.render.RenderSettings");
            var constructor = settingsClass.getConstructor(method.getClass(), encoding.getClass(),
                    int.class, int.class, int.class, int.class, File.class,
                    boolean.class, boolean.class, boolean.class, boolean.class, boolean.class,
                    Class.forName("com.replaymod.lib.de.johni0702.minecraft.gui.utils.lwjgl.ReadableColor"),
                    int.class, int.class, boolean.class, boolean.class, boolean.class, aa.getClass(),
                    String.class, String.class, boolean.class);
            String exportArguments = ((String) encoding.getClass().getMethod("getValue").invoke(encoding))
                    .replace("-b:v %BITRATE%", "-preset fast -crf 18");
            Object settings = constructor.newInstance(method, encoding, 1280, 720, 30, 12000000,
                    output.toFile(), false, false, false, false, false, null, 360, 180,
                    false, false, false, aa, System.getProperty("spycraft.director.ffmpeg", "ffmpeg"),
                    exportArguments, false);
            Class<?> rendererClass = Class.forName("com.replaymod.render.rendering.VideoRenderer");
            Object renderer = rendererClass.getConstructor(settingsClass,
                    Class.forName("com.replaymod.replay.ReplayHandler"), timelineClass)
                    .newInstance(settings, handler, timeline);
            if (!Boolean.TRUE.equals(rendererClass.getMethod("renderVideo").invoke(renderer))) {
                throw new IllegalStateException("ReplayMod render cancelled or failed");
            }
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } finally {
            if (handler != null) {
                try { handler.getClass().getMethod("endReplay").invoke(handler); }
                finally {
                    client.options.hideGui = oldHideGui;
                    client.options.fov().set(oldFov);
                }
            }
        }
    }
    private static Object singleton(String type) throws ReflectiveOperationException {
        Object instance = Class.forName(type).getField("instance").get(null);
        if (instance == null) throw new IllegalStateException(type + " is not initialized");
        return instance;
    }
    private static Object constant(String type, String name) throws ReflectiveOperationException {
        return Class.forName(type).getField(name).get(null);
    }
    private static Object call(Object instance, String method) throws ReflectiveOperationException {
        return instance.getClass().getMethod(method).invoke(instance);
    }
}
