package ro.srth.lbv2.util;

import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class FastFlags {
    private static final Pattern PATTERN = Pattern.compile("^(FInt|FString|FNumber|FBool)(.+)");

    private final Map<String, Object> keys = new HashMap<>();

    public FastFlags() throws IOException {
        final File jsonFile = new File("flags.json");

        if (!jsonFile.exists()) {
            throw new FileNotFoundException("flags.json file does not exist");
        }

        try (var reader = new FileInputStream(jsonFile)) {
            var obj = new JSONObject(new String(reader.readAllBytes()));

            var keys = obj.toMap();

            this.keys.putAll(keys);
        } catch (IOException e) {
            throw new IOException("error reading flags.json", e);
        }
    }

    @Nullable
    public Object query(String query) {
        if (!PATTERN.matcher(query).matches()) {
            return null;
        }

        return this.keys.getOrDefault(query, null);
    }
}
