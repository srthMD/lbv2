package ro.srth.lbv2.handlers;

import org.json.JSONObject;
import ro.srth.lbv2.Bot;

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
            Bot.log.warn("private.json does not exist, all queries will return null.");
            return;
        }

        try (var reader = new FileInputStream(jsonFile)) {
            var obj = new JSONObject(new String(reader.readAllBytes()));

            var keys = obj.getJSONObject("keys");

            keys.toMap().forEach((k, v) -> this.keys.put(k, v.toString()));
        } catch (IOException e) {
            Bot.log.error("error reading private.json, all queries will return null.");
        }
    }

    public String query(String query) {
        return this.keys.getOrDefault(query, null);
    }
}
