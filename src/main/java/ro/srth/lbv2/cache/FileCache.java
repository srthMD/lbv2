package ro.srth.lbv2.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import ro.srth.lbv2.Bot;

import javax.annotation.Nullable;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;

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
                    ((File) value).delete();
                }
            }))
            .build();
    private MessageDigest digest;

    public FileCache() {
        try {
            this.digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Bot.log.warn("MD5 algorithm not available, defaulting to SHA-256.");
            try {
                this.digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    @Override
    public String hash(final String key) {
        return new String(digest.digest(key.getBytes()));
    }

    @Override
    public void put(final String key, final File file) {
        fileCache.put(hash(key), file);
    }

    @Nullable
    public File get(final String key) {
        return fileCache.getIfPresent(hash(key));
    }

    @Override
    public void remove(String key) {
        fileCache.invalidate(hash(key));
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
        return fileCache.asMap().containsKey(hash(key));
    }
}
