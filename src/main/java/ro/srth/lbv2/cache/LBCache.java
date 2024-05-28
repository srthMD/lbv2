package ro.srth.lbv2.cache;

public interface LBCache<K, V> {
    K hash(final K key);

    void put(final K key, final V value);

    V get(final K key);

    void remove(final K key);

    void flush();

    long size();
}
