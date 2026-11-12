package cafe.yamds.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigStoreTest {

    @TempDir
    Path directory;

    @Test
    void createsFileWithDefaultsWhenMissing() {
        Path file = directory.resolve("autofishing.json");
        ConfigStore store = new ConfigStore(file);

        store.load();

        assertTrue(Files.exists(file));
        assertEquals(1.0, store.config().castIntervalSeconds, 1e-9);
        assertEquals(9, store.config().barCurves.size());
        assertTrue(store.curveFor(4).points().size() >= 2);
    }

    @Test
    void roundTripsCustomValues() {
        Path file = directory.resolve("autofishing.json");
        ConfigStore store = new ConfigStore(file);
        store.load();

        store.config().castIntervalSeconds = 2.5;
        store.config().castIntervalRandomSeconds = 0.25;
        store.config().barCurves.put(3, List.of(
                new CurvePoint(0.0, 1.0),
                new CurvePoint(20.0, 0.5),
                new CurvePoint(35.0, 0.0)
        ));
        store.save();

        ConfigStore reloaded = new ConfigStore(file);
        reloaded.load();

        assertEquals(2.5, reloaded.config().castIntervalSeconds, 1e-9);
        assertEquals(0.25, reloaded.config().castIntervalRandomSeconds, 1e-9);
        assertEquals(3, reloaded.config().barCurves.get(3).size());
        assertEquals(1.0, reloaded.curveFor(3).advanceAt(0.0), 1e-9);
    }

    @Test
    void missingFieldsFallBackToDefaults() throws IOException {
        Path file = directory.resolve("legacy.json");
        Files.writeString(file, """
                {
                  "modEnabled": false
                }
                """);

        ConfigStore store = new ConfigStore(file);
        store.load();

        assertFalse(store.config().modEnabled);
        assertEquals(1.0, store.config().castIntervalSeconds, 1e-9);
        assertEquals(0.5, store.config().castIntervalRandomSeconds, 1e-9);
        assertEquals(2.0, store.config().autoRecastMinutes, 1e-9);
        assertEquals(9, store.config().barCurves.size());
    }

    @Test
    void invalidValuesAreNormalized() throws IOException {
        Path file = directory.resolve("invalid.json");
        Files.writeString(file, """
                {
                  "globalAdvance": 99.0,
                  "autoRecastMinutes": -5.0,
                  "castIntervalSeconds": -1.0,
                  "castIntervalRandomSeconds": -3.0,
                  "barCurves": {}
                }
                """);

        ConfigStore store = new ConfigStore(file);
        store.load();

        assertEquals(3.0, store.config().globalAdvance, 1e-9);
        assertEquals(2.0, store.config().autoRecastMinutes, 1e-9);
        assertEquals(1.0, store.config().castIntervalSeconds, 1e-9);
        assertEquals(0.5, store.config().castIntervalRandomSeconds, 1e-9);
        assertEquals(9, store.config().barCurves.size());
    }

    @Test
    void malformedJsonFallsBackToDefaults() throws IOException {
        Path file = directory.resolve("broken.json");
        Files.writeString(file, "{ definitely not json");

        ConfigStore store = new ConfigStore(file);
        store.load();

        assertEquals(9, store.config().barCurves.size());
        assertTrue(store.config().modEnabled);
    }

    @Test
    void curveCacheRefreshesAfterSave() {
        Path file = directory.resolve("cached.json");
        ConfigStore store = new ConfigStore(file);
        store.load();

        store.config().barCurves.put(1, List.of(
                new CurvePoint(0.0, 4.0),
                new CurvePoint(35.0, 4.0)
        ));
        store.save();

        assertEquals(4.0, store.curveFor(1).advanceAt(10.0), 1e-9);
    }
}
