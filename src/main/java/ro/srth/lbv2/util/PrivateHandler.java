package ro.srth.lbv2.util;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to store and query Strings like as API keys or other sensitive data
 * that should not be built into the final JAR.
 */
public class PrivateHandler {
    private final Map<String, String> keys = new HashMap<>();

    public PrivateHandler() {
        final File jsonFile = new File("private.json");

        if (!jsonFile.exists()) {
            throw new RuntimeException("private.json file does not exist");
        }

        try (var reader = new FileInputStream(jsonFile)) {
            var obj = new JSONObject(new String(reader.readAllBytes()));

            var keys = obj.getJSONObject("keys");

            keys.toMap().forEach((k, v) -> this.keys.put(k, v.toString()));
        } catch (IOException e) {
            throw new RuntimeException("error reading private.json");
        }
    }

    @Nullable
    public String query(String query) {
        return this.keys.getOrDefault(query, null);
    }
}
