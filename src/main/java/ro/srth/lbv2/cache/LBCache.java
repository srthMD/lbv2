package ro.srth.lbv2.cache;

/**
 * Interface representing a hash that can be used within the bot.
 *
 * @param <K> The type of the key.
 * @param <V> The type of the value.
 */
public interface LBCache<K, V> {
    /**
     * Generates a hash to be stored as a key.
     */
    K hash(final K key);

    /**
     * Puts a value into the hash. Should call {@link #hash(Object)} to hash the key.
     */
    void put(final K key, final V value);

    /**
     * @param key The key to search with.
     * @return The value associated with the key, or null depending on implementation.
     */
    V get(final K key);

    /**
     * Removes an element from the hash.
     * @param key The key of the value you want to remove.
     */
    void remove(final K key);

    /**
     * Clears the cache, mainly used for when the bot initiates a shutdown, but can be used
     * for other purposes.
     */
    void flush();

    /**
     * Checks if a value exists within the cache.
     * @param key The key associated with the value.
     * @return The truth.
     */
    boolean exists(final K key);

    /**
     * @return The amount of entries within the cache.
     */
    long size();
}
