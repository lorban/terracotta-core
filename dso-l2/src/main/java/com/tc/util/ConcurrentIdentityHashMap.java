/*
 * Copyright (c) 2011-2018 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.tc.util;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConcurrentIdentityHashMap<K, V> implements ConcurrentMap<K, V> {

  private final ConcurrentHashMap<IdentityKey<K>, V> delegate;

  public ConcurrentIdentityHashMap() {
    this.delegate = new ConcurrentHashMap<>();
  }

  public ConcurrentIdentityHashMap(int initialCapacity) {
    this.delegate = new ConcurrentHashMap<>(initialCapacity);
  }

  private IdentityKey<K> ikey(Object k) {
    return new IdentityKey(k);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return delegate.containsKey(ikey(key));
  }

  @Override
  public boolean containsValue(Object value) {
    return delegate.containsValue(value);
  }

  @Override
  public V get(Object key) {
    return delegate.get(ikey(key));
  }

  @Override
  public V put(K key, V value) {
    return delegate.put(ikey(key), value);
  }

  @Override
  public V remove(Object key) {
    return delegate.remove(ikey(key));
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> m) {
    for (Map.Entry<? extends K, ? extends V> ent : m.entrySet()) {
      put(ent.getKey(), ent.getValue());
    }
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public Set<K> keySet() {
    return delegate.keySet().stream().map(key -> key.getKey()).collect(Collectors.toSet());
  }

  @Override
  public Collection<V> values() {
    return delegate.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    return delegate.entrySet()
      .stream()
      .map((Function<Entry<IdentityKey<K>, V>, Entry<K, V>>) entry -> new AbstractMap.SimpleEntry<>(entry.getKey().key,
                                                                                                    entry.getValue()))
      .collect(Collectors.toSet());
  }

  @Override
  public V putIfAbsent(K key, V value) {
    return delegate.putIfAbsent(ikey(key), value);
  }

  @Override
  public boolean remove(Object key, Object value) {
    return delegate.remove(ikey(key), value);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue) {
    return delegate.replace(ikey(key), oldValue, newValue);
  }

  @Override
  public V replace(K key, V value) {
    return delegate.replace(ikey(key), value);
  }

  @Override
  public V getOrDefault(Object key, V defaultValue) {
    return delegate.getOrDefault(ikey(key), defaultValue);
  }

  @Override
  public void forEach(BiConsumer<? super K, ? super V> action) {
    delegate.forEach((k, v) -> action.accept(k.key, v));
  }

  @Override
  public void replaceAll(BiFunction<? super K, ? super V, ? extends V> function) {
    delegate.replaceAll((k, v) -> function.apply(k.key, v));
  }

  @Override
  public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
    return delegate.computeIfAbsent(ikey(key), k -> mappingFunction.apply(k.key));
  }

  @Override
  public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    return delegate.computeIfPresent(ikey(key), (k, v) -> remappingFunction.apply(k.key, v));
  }

  @Override
  public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
    return delegate.compute(ikey(key), (k, v) -> remappingFunction.apply(k.key, v));
  }

  @Override
  public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
    return delegate.merge(ikey(key), value, remappingFunction);
  }

  static class IdentityKey<KK> {
    final KK key;

    IdentityKey(KK key) {
      this.key = key;
    }

    public KK getKey() {
      return key;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof IdentityKey) {
        return ((IdentityKey) o).key == this.key;
      }
      if (key.getClass().isInstance(o)) {
        return key == o;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(key);
    }
  }
}
