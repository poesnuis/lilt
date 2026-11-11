package dev.lilt;

import dev.lilt.config.*;
import dev.lilt.core.WidthCache;
import dev.lilt.metrics.*;
import dev.lilt.module.ModuleDefinition;
import dev.lilt.ui.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.*;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Future;

@Mod(modid = LiltClient.ID, name = "Lilt Client", version = "1.0.0", acceptedMinecraftVersions = "[1.12.2]", clientSideOnly = true, acceptableRemoteVersions = "*")
public final class LiltClient {
    public static final String ID = "lilt";
    @Mod.Instance(ID) public static LiltClient instance;
    public Config config = Config.defaults();
    public ConfigStore store;
    public String activeProfile = "default", notice = "";
    public final FrameMetrics frames = new FrameMetrics();
    public final SystemMetrics system = new SystemMetrics();
    public final long sessionStarted = System.nanoTime();
    public BenchmarkRecorder recorder;
    public HudRenderer hud;
    private long lastSystemSample;
    private Future<Config> loading;
    private String loadingName;
    private final KeyBinding studioKey = new KeyBinding("key.lilt.studio", Keyboard.KEY_RSHIFT, "key.categories.lilt");
    private final KeyBinding editorKey = new KeyBinding("key.lilt.editor", Keyboard.KEY_F9, "key.categories.lilt");
    private final KeyBinding captureKey = new KeyBinding("key.lilt.capture", Keyboard.KEY_F8, "key.categories.lilt");
    private static final int MENU_BUTTON = 77412;

    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        Path directory = event.getModConfigurationDirectory().toPath().resolve("lilt");
        try {
            store = new ConfigStore(directory);
            activeProfile = store.activeName();
            config = store.loadInitial(activeProfile);
            if (!store.message().equals("All changes saved")) notice = store.message();
            store.save(activeProfile, config);
        } catch (IOException exception) { notice = "Settings are temporary: " + exception.getMessage(); }
        recorder = new BenchmarkRecorder(directory.resolve("benchmarks"));
        WidthCache.setEnabled(config.module(ModuleDefinition.TEXT_CACHE).enabled);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (store != null) store.close();
            recorder.close();
        }, "Lilt shutdown"));
    }
    @Mod.EventHandler public void init(FMLInitializationEvent event) {
        hud = new HudRenderer(this);
        ClientRegistry.registerKeyBinding(studioKey);
        ClientRegistry.registerKeyBinding(editorKey);
        ClientRegistry.registerKeyBinding(captureKey);
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new LiltCommand(this));
    }
    public void changed() {
        WidthCache.setEnabled(config.module(ModuleDefinition.TEXT_CACHE).enabled);
        if (store != null) store.save(activeProfile, config);
    }
    public boolean loading() { return loading != null; }
    public void loadProfile(String name) {
        if (store == null || loading()) return;
        loadingName = name; loading = store.loadAsync(name); notice = "Loading " + name + "…";
    }
    public void importProfile() {
        if (store == null || loading()) return;
        loadingName = "imported"; loading = store.importAsync(); notice = "Reading config/lilt/import.json…";
    }
    public void createProfile(String name) {
        if (store == null || loading()) return;
        if (!ConfigStore.validName(name)) { notice = "Use 1–32 letters, numbers, dashes or underscores."; return; }
        // Ensure the previous profile is durable before switching the autosave target.
        store.save(activeProfile, config);
        activeProfile = name; changed(); notice = "Saved snapshot as " + name;
    }
    public void toggle(ModuleDefinition definition) {
        ModuleConfig module = config.module(definition);
        module.enabled = !module.enabled;
        if (definition.hud && module.enabled) {
            ScaledResolution screen = new ScaledResolution(Minecraft.getMinecraft());
            hud.avoidOverlap(definition, screen.getScaledWidth(), screen.getScaledHeight());
        }
        changed();
    }
    public void applyLayout(String name) {
        ScaledResolution screen = new ScaledResolution(Minecraft.getMinecraft());
        notice = ""; hud.arrange(name, screen.getScaledWidth(), screen.getScaledHeight()); changed();
    }
    @SubscribeEvent public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        long now = System.nanoTime();
        if (now - lastSystemSample >= 1_000_000_000L) { system.sample(now); lastSystemSample = now; }
        if (loading != null && loading.isDone()) {
            try { config = loading.get(); activeProfile = loadingName; changed(); notice = "Loaded " + activeProfile; }
            catch (Exception failure) { notice = "Could not load profile: " + (failure.getCause() == null ? failure : failure.getCause()).getMessage(); }
            loading = null;
        }
        hud.update(now);
    }
    @SubscribeEvent public void renderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getMinecraft();
        long now = System.nanoTime();
        long delta = frames.record(now);
        recorder.frame(delta, mc.world != null, mc.currentScreen != null, now, system);
    }
    @SubscribeEvent public void overlay(RenderGameOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.world == null || mc.gameSettings.hideGUI
            || (mc.gameSettings.showDebugInfo && !config.showWithDebug) || mc.currentScreen instanceof StudioScreen || mc.currentScreen instanceof HudEditorScreen) return;
        Canvas.begin();
        try { hud.render(event.getResolution().getScaledWidth(), event.getResolution().getScaledHeight(), false); }
        finally { Canvas.end(); }
    }
    @SubscribeEvent public void key(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) return;
        if (studioKey.isPressed()) { mc.displayGuiScreen(new StudioScreen(null)); return; }
        if (editorKey.isPressed()) { mc.displayGuiScreen(new HudEditorScreen(null)); return; }
        if (captureKey.isPressed()) { capture("Survival", 60); return; }
        if (Keyboard.getEventKeyState() && !Keyboard.isRepeatEvent()) {
            int key = Keyboard.getEventKey();
            if (key != 0) for (ModuleDefinition definition : ModuleDefinition.values()) if (config.module(definition).keybind == key) toggle(definition);
        }
    }
    @SubscribeEvent public void menu(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiMainMenu || event.getGui() instanceof GuiIngameMenu) {
            event.getButtonList().add(new GuiButton(MENU_BUTTON, event.getGui().width - 110, 8, 102, 20, "Lilt Studio"));
        }
    }
    @SubscribeEvent public void menuAction(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getButton().id == MENU_BUTTON) Minecraft.getMinecraft().displayGuiScreen(new StudioScreen(event.getGui()));
    }
    public void capture(String scenario, int seconds) {
        if (recorder.active()) { recorder.stop("user stopped"); return; }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null) { notice = "Join a world before starting a capture."; return; }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("client", "Lilt 1.0.0 / Forge 14.23.5.2860 / Minecraft 1.12.2");
        data.put("profile", activeProfile);
        data.put("java", System.getProperty("java.runtime.version"));
        data.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
        data.put("logicalProcessors", Runtime.getRuntime().availableProcessors());
        data.put("maxHeapBytes", Runtime.getRuntime().maxMemory());
        data.put("glVendor", GL11.glGetString(GL11.GL_VENDOR));
        data.put("glRenderer", GL11.glGetString(GL11.GL_RENDERER));
        data.put("glVersion", GL11.glGetString(GL11.GL_VERSION));
        data.put("displayWidth", mc.displayWidth); data.put("displayHeight", mc.displayHeight);
        data.put("renderDistanceChunks", mc.gameSettings.renderDistanceChunks);
        data.put("fancyGraphics", mc.gameSettings.fancyGraphics);
        data.put("ambientOcclusion", mc.gameSettings.ambientOcclusion);
        data.put("particles", mc.gameSettings.particleSetting);
        data.put("mipmapLevels", mc.gameSettings.mipmapLevels);
        data.put("vsync", mc.gameSettings.enableVsync);
        data.put("frameLimit", mc.gameSettings.limitFramerate);
        data.put("renderClouds", mc.gameSettings.clouds);
        data.put("entityShadows", mc.gameSettings.entityShadows);
        data.put("resourcePacks", new ArrayList<>(mc.gameSettings.resourcePacks));
        data.put("singleplayer", mc.isSingleplayer());
        data.put("textCacheEnabled", config.module(ModuleDefinition.TEXT_CACHE).enabled);
        data.put("textCacheInstalled", WidthCache.isInstalled());
        data.put("configuration", ConfigStore.JSON.toJsonTree(config));
        if (!recorder.start(scenario, seconds, data, system)) notice = "Wait for the previous capture to finish saving.";
        else notice = "Capture started; close the Studio to measure gameplay.";
    }
}
