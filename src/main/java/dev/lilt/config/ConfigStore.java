package dev.lilt.config;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Owns disk IO. Only detached JSON snapshots cross from the game thread to this worker. */
public final class ConfigStore implements AutoCloseable {
    public static final int MAX_BYTES = 1024 * 1024;
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path root;
    private final ScheduledExecutorService worker = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "Lilt configuration"); thread.setDaemon(true); return thread;
    });
    private ScheduledFuture<?> pending;
    private Runnable pendingWrite;
    private String pendingName;
    private volatile String message = "All changes saved";
    public ConfigStore(Path root) throws IOException { this.root = root; Files.createDirectories(root.resolve("profiles")); }
    public Path root() { return root; }
    public String message() { return message; }
    public static boolean validName(String name) { return name != null && name.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,31}"); }
    private Path profile(String name) {
        if (!validName(name)) throw new IllegalArgumentException("Use 1–32 letters, numbers, dashes or underscores.");
        return root.resolve("profiles").resolve(name + ".json");
    }
    public String activeName() {
        try {
            if (Files.size(root.resolve("active.txt")) > 128) return "default";
            String name = new String(Files.readAllBytes(root.resolve("active.txt")), StandardCharsets.UTF_8).trim();
            return validName(name) ? name : "default";
        } catch (IOException ignored) { return "default"; }
    }
    public Config loadInitial(String name) {
        Path path = profile(name);
        if (!Files.exists(path)) return Config.defaults();
        try { return read(path); }
        catch (Exception failure) {
            message = "Recovered defaults; invalid profile preserved on disk";
            try {
                Files.copy(path, path.resolveSibling(name + ".invalid-" + System.currentTimeMillis() + ".json"));
                Path backup = path.resolveSibling(name + ".json.bak");
                if (Files.exists(backup)) { Config result = read(backup); message = "Recovered last valid backup"; return result; }
            } catch (Exception ignored) { }
            return Config.defaults();
        }
    }
    public static Config read(Path path) throws IOException {
        if (Files.isSymbolicLink(path)) throw new IOException("Symbolic links are not accepted for profile imports.");
        long size = Files.size(path);
        if (size > MAX_BYTES) throw new IOException("Profile exceeds 1 MiB.");
        String text = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        int depth = 0; boolean quoted = false, escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (escaped) escaped = false;
                else if (c == '\\') escaped = true;
                else if (c == '"') quoted = false;
            } else if (c == '"') quoted = true;
            else if (c == '{' || c == '[') { if (++depth > 32) throw new IOException("Profile is nested too deeply."); }
            else if (c == '}' || c == ']') depth--;
        }
        try {
            JsonElement tree = new JsonParser().parse(text);
            if (!tree.isJsonObject()) throw new IOException("Profile must be a JSON object.");
            JsonObject object = tree.getAsJsonObject();
            if (!object.has("schemaVersion")) throw new IOException("Missing schemaVersion.");
            if (object.has("modules") && !object.get("modules").isJsonNull() && !object.get("modules").isJsonObject()) throw new IOException("Modules must be a JSON object.");
            Config config = JSON.fromJson(object, Config.class);
            config.validate();
            return config;
        } catch (RuntimeException exception) { throw new IOException("Invalid profile: " + exception.getMessage(), exception); }
    }
    public synchronized void save(String name, Config config) {
        final Path path = profile(name);
        if (pendingName != null && !pendingName.equals(name)) flushPending();
        // The caller owns the mutable model; make the snapshot before scheduling.
        final String snapshot = JSON.toJson(config);
        if (pending != null) pending.cancel(false);
        pendingWrite = () -> {
            try {
                backupIfValid(path);
                atomicWrite(path, snapshot);
                atomicWrite(root.resolve("active.txt"), name + "\n");
                message = "All changes saved";
            } catch (IOException failure) { message = "Save failed: " + failure.getMessage(); }
        };
        message = "Saving changes…";
        pending = worker.schedule(pendingWrite, 500, TimeUnit.MILLISECONDS);
        pendingName = name;
    }
    public synchronized Future<Config> loadAsync(String name) {
        flushPending();
        final Path path = profile(name);
        return worker.submit(() -> read(path));
    }
    public synchronized Future<Config> importAsync() {
        flushPending();
        return worker.submit(() -> read(root.resolve("import.json")));
    }
    public Future<List<String>> listAsync() {
        return worker.submit(() -> {
            List<String> names = new ArrayList<>();
            try (DirectoryStream<Path> files = Files.newDirectoryStream(root.resolve("profiles"), "*.json")) {
                for (Path file : files) {
                    String name = file.getFileName().toString().replaceFirst("\\.json$", "");
                    if (validName(name)) names.add(name);
                }
            }
            Collections.sort(names);
            return names;
        });
    }
    public Future<?> exportAsync(String name, Config config) {
        if (!validName(name)) throw new IllegalArgumentException("Invalid export name");
        String snapshot = JSON.toJson(config);
        return worker.submit(() -> {
            try {
                Files.createDirectories(root.resolve("exports"));
                atomicWrite(root.resolve("exports").resolve(name + ".json"), snapshot);
                message = "Exported to config/lilt/exports/" + name + ".json";
            } catch (IOException failure) { message = "Export failed: " + failure.getMessage(); }
        });
    }
    private synchronized void flushPending() {
        if (pending != null && pending.cancel(false) && pendingWrite != null) worker.submit(pendingWrite);
        pending = null; pendingWrite = null; pendingName = null;
    }
    private static void backupIfValid(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try { read(path); }
        catch (IOException invalid) { return; }
        Files.copy(path, path.resolveSibling(path.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
    }
    public static void atomicWrite(Path path, String text) throws IOException {
        Files.createDirectories(path.getParent());
        Path temp = Files.createTempFile(path.getParent(), ".lilt-", ".tmp");
        try {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            try (FileOutputStream stream = new FileOutputStream(temp.toFile())) { stream.write(bytes); stream.getFD().sync(); }
            try { Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
    public synchronized void close() {
        flushPending();
        worker.shutdown();
        try { worker.awaitTermination(5, TimeUnit.SECONDS); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
    }
}
