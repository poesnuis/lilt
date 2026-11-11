package dev.lilt.config;

import dev.lilt.module.ModuleDefinition;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

public class ConfigStoreTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void pendingEditsSurviveSwitchingProfilesAndSnapshotsAreIsolated() throws Exception {
        Path root = temporary.newFolder().toPath();
        try (ConfigStore store = new ConfigStore(root)) {
            Config config = Config.defaults();
            config.module(ModuleDefinition.FPS).x = .31;
            store.save("first", config);
            config.module(ModuleDefinition.FPS).x = .72;
            store.save("second", config);
            config.module(ModuleDefinition.FPS).x = .99;
            assertEquals(.31, store.loadAsync("first").get(5, TimeUnit.SECONDS).module(ModuleDefinition.FPS).x, .00001);
            assertEquals(.72, store.loadAsync("second").get(5, TimeUnit.SECONDS).module(ModuleDefinition.FPS).x, .00001);
        }
        assertEquals("second", new String(Files.readAllBytes(root.resolve("active.txt")), StandardCharsets.UTF_8).trim());
    }
    @Test public void corruptProfileRecoversBackupAndPreservesEvidence() throws Exception {
        Path root = temporary.newFolder().toPath();
        try (ConfigStore store = new ConfigStore(root)) {
            Config expected = Config.defaults(); expected.primary = "ABCDEF";
            store.save("default", expected); store.loadAsync("default").get();
            expected.primary = "123456";
            store.save("default", expected); store.loadAsync("default").get();
            Files.write(root.resolve("profiles/default.json"), "{ broken".getBytes(StandardCharsets.UTF_8));
            assertEquals("ABCDEF", store.loadInitial("default").primary);
            assertTrue(store.listAsync().get().contains("default"));
            try (DirectoryStream<Path> invalid = Files.newDirectoryStream(root.resolve("profiles"), "default.invalid-*.json")) { assertTrue(invalid.iterator().hasNext()); }
        }
    }
    @Test public void invalidOrFutureProfilesAreRejectedWithoutReplacingActiveConfiguration() throws Exception {
        Path file = temporary.newFile().toPath();
        for (String text : new String[]{"null", "[]", "{}", "{\"schemaVersion\":2}", "{\"schemaVersion\":1,\"modules\":[]}"}) {
            Files.write(file, text.getBytes(StandardCharsets.UTF_8));
            try { ConfigStore.read(file); fail(text); } catch (java.io.IOException expected) { }
        }
        Files.write(file, new byte[ConfigStore.MAX_BYTES + 1]);
        try { ConfigStore.read(file); fail("Oversized profile accepted"); } catch (java.io.IOException expected) { }
    }
    @Test public void traversalIsRejectedAndValuesAreClamped() {
        for (String name : new String[]{"../escape", "a/b", "", "C:\\path", ".hidden", "test.json", "x\n"}) assertFalse(name, ConfigStore.validName(name));
        assertTrue(ConfigStore.validName("Desk-2_v3"));
        Config config = Config.defaults();
        ModuleConfig fps = config.module(ModuleDefinition.FPS);
        fps.x = Double.NaN; fps.appearance.opacity = 300; fps.appearance.scale = -2;
        fps.appearance.backgroundColor = "oops"; fps.appearance.font = null; fps.appearance.align = null;
        config.validate();
        assertEquals(.02, fps.x, 0); assertEquals(1, fps.appearance.opacity, 0); assertEquals(.5, fps.appearance.scale, 0);
        assertEquals("182B36", fps.appearance.backgroundColor); assertEquals("Open Sans", fps.appearance.font);
        assertEquals("LEFT", fps.appearance.align);
    }
}
