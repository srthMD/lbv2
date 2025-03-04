package ro.srth.lbv2.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import javax.annotation.Nullable;
import java.io.File;
import java.time.Duration;

/**
 * Basic class holding a cache that stores references to files.
 * The hashes are supposed to be generated using the file's name and size.
 */
public final class FileCache implements LBCache<String, File> {
    private final Cache<String, File> fileCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .expireAfterAccess(Duration.ofSeconds(45))
            .maximumWeight(102400)
            .weigher(((key, value) ->
                    ((int) ((File) value).length()) / 1024)
            )
            .initialCapacity(10)
            .evictionListener(((key, value, cause) -> {
                if (value != null) {
                    //noinspection ResultOfMethodCallIgnored
                    ((File) value).delete();
                }
            }))
            .build();

    @Override
    public void put(final String key, final File file) {
        fileCache.put(key, file);
    }

    @Nullable
    public File get(final String key) {
        return fileCache.getIfPresent(key);
    }

    @Override
    public void remove(String key) {
        fileCache.invalidate(key);
    }

    @Override
    public void flush() {
        fileCache.invalidateAll();
    }

    @Override
    public long size() {
        return fileCache.estimatedSize();
    }

    @Override
    public boolean exists(final String key) {
        return fileCache.asMap().containsKey(key);
    }
}
