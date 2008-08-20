/**
 * Copyright 2008 Atlassian Pty Ltd 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package com.atlassian.util.concurrent;

import static com.atlassian.util.concurrent.Assertions.notNull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
abstract class AbstractCopyOnWriteMap<K, V, M extends Map<K, V>> implements Map<K, V>, Serializable {
    private static final long serialVersionUID = 4508989182041753878L;

    private volatile M delegate;
    private final CopyFunction<M> factory;
    private final transient EntrySet entrySet = new EntrySet();
    private final transient KeySet keySet = new KeySet();
    private final transient Values values = new Values();

    /**
     * Create a new {@link CopyOnWriteMap} with the supplied {@link Map} to
     * initialize the values and the {@link CopyFunction} for creating our
     * actual delegate instances.
     * 
     * @param map the initial map to initialize with
     * @param factory the copy function
     */
    protected AbstractCopyOnWriteMap(final M map, final CopyFunction<M> factory) {
        this.factory = notNull("CopyFunction", factory);
        this.delegate = notNull("delegate", factory.copy(notNull("map", map)));
    }

    //
    // mutable operations
    //

    public synchronized final void clear() {
        final M map = copy();
        map.clear();
        set(map);
    }

    public synchronized final V remove(final Object key) {
        // short circuit if key doesn't exist
        if (!delegate.containsKey(key)) {
            return null;
        }
        final M map = copy();
        final V result = map.remove(key);
        set(map);
        return result;
    }

    public synchronized final V put(final K key, final V value) {
        final M map = copy();
        final V result = map.put(key, value);
        set(map);
        return result;
    }

    public synchronized final void putAll(final Map<? extends K, ? extends V> t) {
        final M map = copy();
        map.putAll(t);
        set(map);
    }

    protected synchronized void removeAll(final Collection<K> keys) {
        final M map = copy();
        for (final K k : keys) {
            map.remove(k);
        }
        set(map);
    }

    protected synchronized M copy() {
        return factory.copy(delegate);
    }

    protected synchronized void set(final M map) {
        delegate = map;
    }

    //
    // Collection views
    //

    public final Set<Map.Entry<K, V>> entrySet() {
        return entrySet;
    }

    public final Set<K> keySet() {
        return keySet;
    }

    public final Collection<V> values() {
        return values;
    }

    //
    // delegate operations
    //

    public final boolean containsKey(final Object key) {
        return delegate.containsKey(key);
    }

    public final boolean containsValue(final Object value) {
        return delegate.containsValue(value);
    }

    public final V get(final Object key) {
        return delegate.get(key);
    }

    public final boolean isEmpty() {
        return delegate.isEmpty();
    }

    public final int size() {
        return delegate.size();
    }

    @Override
    public final boolean equals(final Object o) {
        return delegate.equals(o);
    }

    @Override
    public final int hashCode() {
        return delegate.hashCode();
    }

    protected final M getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    //
    // inner interfaces
    //

    /**
     * Copy the current map. Always done under a lock so we don't get multiple
     * threads doing this concurrently.
     */
    protected interface CopyFunction<M extends Map<?, ?>> {
        /**
         * Create a new map copied from the one supplied. Implementations should
         * not keep a reference to this map, and must not modify the map after
         * it has been returned. This will be called under synchronization, so
         * it should not do any IO or blocking operations.
         * 
         * @param map the map to copy. Will not be null.
         * @return a new copied map. Must not be null.
         */
        M copy(M map);
    }

    //
    // inner classes
    //

    private class KeySet extends CollectionView<K> implements Set<K> {

        @Override
        Collection<K> getDelegate() {
            return delegate.keySet();
        }

        //
        // mutable operations
        //

        public void clear() {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                map.keySet().clear();
                set(map);
            }
        }

        public boolean remove(final Object o) {
            return AbstractCopyOnWriteMap.this.remove(o) != null;
        }

        public boolean removeAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.keySet().removeAll(c);
                set(map);
                return result;
            }
        }

        public boolean retainAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.keySet().retainAll(c);
                set(map);
                return result;
            }
        }
    }

    private final class Values extends CollectionView<V> implements Collection<V> {

        @Override
        Collection<V> getDelegate() {
            return delegate.values();
        }

        public void clear() {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                map.values().clear();
                set(map);
            }
        }

        public boolean remove(final Object o) {
            synchronized (AbstractCopyOnWriteMap.this) {
                if (!contains(o)) {
                    return false;
                }
                final M map = copy();
                final boolean result = map.values().remove(o);
                set(map);
                return result;
            }
        }

        public boolean removeAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.values().removeAll(c);
                set(map);
                return result;
            }
        }

        public boolean retainAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.values().retainAll(c);
                set(map);
                return result;
            }
        }
    }

    private class EntrySet extends CollectionView<Entry<K, V>> implements Set<Map.Entry<K, V>> {

        @Override
        Collection<java.util.Map.Entry<K, V>> getDelegate() {
            return delegate.entrySet();
        }

        public void clear() {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                map.entrySet().clear();
                set(map);
            }
        }

        public boolean remove(final Object o) {
            synchronized (AbstractCopyOnWriteMap.this) {
                if (!contains(o)) {
                    return false;
                }
                final M map = copy();
                final boolean result = map.entrySet().remove(o);
                set(map);
                return result;
            }
        }

        public boolean removeAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.entrySet().removeAll(c);
                set(map);
                return result;
            }
        }

        public boolean retainAll(final Collection<?> c) {
            synchronized (AbstractCopyOnWriteMap.this) {
                final M map = copy();
                final boolean result = map.entrySet().retainAll(c);
                set(map);
                return result;
            }
        }
    }

    private static class UnmodifiableIterator<T> implements Iterator<T> {
        private final Iterator<T> delegate;

        public UnmodifiableIterator(final Iterator<T> delegate) {
            this.delegate = delegate;
        }

        public boolean hasNext() {
            return delegate.hasNext();
        }

        public T next() {
            return delegate.next();
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    protected static abstract class CollectionView<E> implements Collection<E> {

        abstract Collection<E> getDelegate();

        //
        // delegate operations
        //

        public final boolean contains(final Object o) {
            return getDelegate().contains(o);
        }

        public final boolean containsAll(final Collection<?> c) {
            return getDelegate().containsAll(c);
        }

        public final Iterator<E> iterator() {
            return new UnmodifiableIterator<E>(getDelegate().iterator());
        }

        public final boolean isEmpty() {
            return getDelegate().isEmpty();
        }

        public final int size() {
            return getDelegate().size();
        }

        public final Object[] toArray() {
            return getDelegate().toArray();
        }

        public final <T> T[] toArray(final T[] a) {
            return getDelegate().toArray(a);
        }

        @Override
        public int hashCode() {
            return getDelegate().hashCode();
        }

        @Override
        public boolean equals(final Object obj) {
            return getDelegate().equals(obj);
        }

        //
        // unsupported operations
        //

        public final boolean add(final E o) {
            throw new UnsupportedOperationException();
        }

        public final boolean addAll(final Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }
    }
}