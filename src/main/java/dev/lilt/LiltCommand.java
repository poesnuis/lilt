package dev.lilt;

import dev.lilt.config.ConfigStore;
import dev.lilt.core.WidthCache;
import dev.lilt.module.ModuleDefinition;
import dev.lilt.ui.StudioScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.command.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class LiltCommand extends CommandBase {
    private final LiltClient client;
    private static volatile long blackhole;
    public LiltCommand(LiltClient client) { this.client = client; }
    public String getName() { return "lilt"; }
    public String getUsage(ICommandSender sender) { return "/lilt [capture|stop|fontcheck]"; }
    public int getRequiredPermissionLevel() { return 0; }
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        Minecraft mc = Minecraft.getMinecraft();
        if (args.length == 0) { mc.addScheduledTask(() -> mc.displayGuiScreen(new StudioScreen(null))); return; }
        if (args[0].equals("capture")) { client.capture("Survival", 60); sender.sendMessage(new TextComponentString(client.recorder.status)); return; }
        if (args[0].equals("stop")) { client.recorder.stop("command"); return; }
        if (!args[0].equals("fontcheck")) throw new WrongUsageException(getUsage(sender));
        if (client.store == null) throw new CommandException("Configuration directory unavailable");
        Map<String, Object> result = fontCheck(mc.fontRenderer);
        final Path output = client.store.root().resolve("benchmarks/font-check.json");
        CompletableFuture.runAsync(() -> {
            try { ConfigStore.atomicWrite(output, ConfigStore.JSON.toJson(result)); }
            catch (Exception failure) { mc.addScheduledTask(() -> sender.sendMessage(new TextComponentString("Lilt font check could not be saved: " + failure.getMessage()))); }
        });
        sender.sendMessage(new TextComponentString("Lilt font check: " + result.get("parityChecks") + " exact matches. Results: config/lilt/benchmarks/font-check.json"));
    }
    private Map<String, Object> fontCheck(FontRenderer font) throws CommandException {
        boolean unicode = font.getUnicodeFlag(), bidi = font.getBidiFlag();
        boolean enabled = client.config.module(ModuleDefinition.TEXT_CACHE).enabled;
        String[] examples = {null, "", "Lilt Client 1.12.2", "\u00a7lBold \u00a7rregular", "\u00a7aGreen \u00a7rReset", "A\tB", "Hello, world!", "\u4e2d\u6587", "\u65e5\u672c\u8a9e", "\u05e9\u05dc\u05d5\u05dd", "\u00a7nUnderline", "\u00a7oItalic", "\u00a7mStrike", "\u00a7kObfuscated", "\u2764", "\u00a7", "\u00a7l\u00a7lRepeated bold", "\u00a71\u00a7lColored bold"};
        Map<String, Object> result = new LinkedHashMap<>();
        int checks = 0;
        try {
            for (boolean u : new boolean[]{false, true}) for (boolean b : new boolean[]{false, true}) {
                font.setUnicodeFlag(u); font.setBidiFlag(b);
                for (String text : examples) {
                    WidthCache.setEnabled(false); int expected = font.getStringWidth(text);
                    WidthCache.setEnabled(true);
                    if (font.getStringWidth(text) != expected || font.getStringWidth(text) != expected) throw new CommandException("Font width parity failure");
                    checks++;
                }
            }
            font.setUnicodeFlag(unicode); font.setBidiFlag(bidi);
            String[] corpus = new String[128];
            for (int i = 0; i < corpus.length; i++) corpus[i] = "\u00a7fPlayer " + i + " \u00a77/ Survival scoreboard / X:128 Y:64 Z:-32";
            List<Map<String, Object>> timings = new ArrayList<>();
            for (int round = 0; round < 5; round++) {
                Map<String, Object> row = new LinkedHashMap<>();
                // Alternate ordering to reduce warmup/order bias; still a microbenchmark.
                boolean first = round % 2 == 0;
                for (boolean cache : new boolean[]{first, !first}) {
                    WidthCache.setEnabled(cache);
                    measure(font, corpus, 100_000);
                    row.put(cache ? "cachedNsPerLookup" : "uncachedNsPerLookup", measure(font, corpus, 500_000) / 500_000.0);
                }
                timings.add(row);
            }
            result.put("installed", WidthCache.isInstalled()); result.put("parityChecks", checks);
            result.put("method", "Actual Minecraft FontRenderer, 128 repeated scoreboard-style strings, 100000 warmup calls and 500000 timed lookups per sample; five alternating-order rounds.");
            result.put("scope", "Text-width microbenchmark only. This does not establish whole-game FPS improvement. Run deliberately; it briefly blocks the game thread.");
            result.put("java", System.getProperty("java.runtime.version")); result.put("rounds", timings);
        } finally { font.setUnicodeFlag(unicode); font.setBidiFlag(bidi); WidthCache.setEnabled(enabled); }
        return result;
    }
    private static long measure(FontRenderer font, String[] corpus, int iterations) {
        long sum = 0, start = System.nanoTime();
        for (int i = 0; i < iterations; i++) sum += font.getStringWidth(corpus[i & 127]);
        long elapsed = System.nanoTime() - start; blackhole = sum; return elapsed;
    }
}
