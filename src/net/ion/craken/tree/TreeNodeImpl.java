package net.ion.craken.tree;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.AtomicHashMapProxy;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.batch.BatchContainer;
import org.infinispan.context.Flag;
import org.infinispan.util.Immutables;
import org.infinispan.util.Util;

public class TreeNodeImpl<K, V> extends TreeStructureSupport implements TreeNode<K, V> {
	private Fqn fqn;
	private TreeNodeKey dataKey, structureKey;

	public TreeNodeImpl(Fqn fqn, AdvancedCache<?, ?> cache, BatchContainer batchContainer) {
		super(cache, batchContainer);
		this.fqn = fqn;
		dataKey = new TreeNodeKey(fqn, TreeNodeKey.Type.DATA);
		structureKey = new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE);
	}

	@Override
	public TreeNode<K, V> getParent() {
		return getParent(cache);
	}

	@Override
	public TreeNode<K, V> getParent(Flag... flags) {
		return getParent(cache.withFlags(flags));
	}

	private TreeNode<K, V> getParent(AdvancedCache<?, ?> cache) {
		if (fqn.isRoot())
			return this;
		return new TreeNodeImpl<K, V>(fqn.getParent(), cache, batchContainer);
	}

	@Override
	public Set<TreeNode<K, V>> getChildren() {
		return getChildren(cache);
	}

	@Override
	public Set<TreeNode<K, V>> getChildren(Flag... flags) {
		return getChildren(cache.withFlags(flags));
	}

	private Set<TreeNode<K, V>> getChildren(AdvancedCache<?, ?> cache) {
		startAtomic();
		try {
			Set<TreeNode<K, V>> result = new HashSet<TreeNode<K, V>>();
			for (Fqn f : getStructure().values()) {
				TreeNodeImpl<K, V> n = new TreeNodeImpl<K, V>(f, cache, batchContainer);
				result.add(n);
			}
			return Immutables.immutableSetWrap(result);
		} finally {
			endAtomic();
		}
	}

	@Override
	public Set<Object> getChildrenNames() {
		return getChildrenNames(cache);
	}

	@Override
	public Set<Object> getChildrenNames(Flag... flags) {
		return getChildrenNames(cache.withFlags(flags));
	}

	private Set<Object> getChildrenNames(AdvancedCache<?, ?> cache) {
		return Immutables.immutableSetCopy(getStructure(cache).keySet());
	}

	@Override
	public Map<K, V> getData() {
		return getData(cache);
	}

	@Override
	public Map<K, V> getData(Flag... flags) {
		return getData(cache.withFlags(flags));
	}

	private Map<K, V> getData(AdvancedCache<?, ?> cache) {
		return Collections.unmodifiableMap(new HashMap<K, V>(getDataInternal(cache)));
	}

	@Override
	public Set<K> getKeys() {
		return getKeys(cache);
	}

	@Override
	public Set<K> getKeys(Flag... flags) {
		return getKeys(cache.withFlags(flags));
	}

	private Set<K> getKeys(AdvancedCache<?, ?> cache) {
		startAtomic();
		try {
			return getData(cache).keySet();
		} finally {
			endAtomic();
		}
	}

	@Override
	public Fqn getFqn() {
		return fqn;
	}

	@Override
	public TreeNode<K, V> addChild(Fqn f) {
		return addChild(cache, f);
	}

	@Override
	public TreeNode<K, V> addChild(Fqn f, Flag... flags) {
		return addChild(cache.withFlags(flags), f);
	}

	private TreeNode<K, V> addChild(AdvancedCache<?, ?> cache, Fqn f) {
		startAtomic();
		try {
			Fqn absoluteChildFqn = Fqn.fromRelativeFqn(fqn, f);

			// 1) first register it with the parent
			AtomicMap<Object, Fqn> structureMap = getStructure(cache);
			structureMap.put(f.getLastElement(), absoluteChildFqn);

			// 2) then create the structure and data maps
			createNodeInCache(cache, absoluteChildFqn);

			final TreeNodeImpl result = new TreeNodeImpl<K, V>(absoluteChildFqn, cache, batchContainer);

			return result;
		} finally {
			endAtomic();
		}
	}

	@Override
	public boolean removeChild(Fqn f) {
		return removeChild(cache, f);
	}

	@Override
	public boolean removeChild(Fqn f, Flag... flags) {
		return removeChild(cache.withFlags(flags), f);
	}

	public boolean removeChild(AdvancedCache<?, ?> cache, Fqn f) {
		return removeChild(cache, f.getLastElement());
	}

	@Override
	public boolean removeChild(Object childName) {
		return removeChild(cache, childName);
	}

	@Override
	public boolean removeChild(Object childName, Flag... flags) {
		return removeChild(cache.withFlags(flags), childName);
	}

	private boolean removeChild(AdvancedCache cache, Object childName) {
		startAtomic();
		try {
			AtomicMap<Object, Fqn> s = getStructure(cache);
			Fqn childFqn = s.remove(childName);
			if (childFqn != null) {
				TreeNode<K, V> child = new TreeNodeImpl<K, V>(childFqn, cache, batchContainer);
				child.removeChildren();
				child.clearData(); // this is necessary in case we have a remove and then an add on the same node, in the same tx.
				cache.remove(new TreeNodeKey(childFqn, TreeNodeKey.Type.DATA));
				cache.remove(new TreeNodeKey(childFqn, TreeNodeKey.Type.STRUCTURE));
				return true;
			}

			return false;
		} finally {
			endAtomic();
		}
	}

	@Override
	public TreeNode<K, V> getChild(Fqn f) {
		return getChild(cache, f);
	}

	@Override
	public TreeNode<K, V> getChild(Fqn f, Flag... flags) {
		return getChild(cache.withFlags(flags), f);
	}

	private TreeNode<K, V> getChild(AdvancedCache cache, Fqn f) {
		startAtomic();
		try {
			if (hasChild(f))
				return new TreeNodeImpl<K, V>(Fqn.fromRelativeFqn(fqn, f), cache, batchContainer);
			else
				return null;
		} finally {
			endAtomic();
		}
	}

	@Override
	public TreeNode<K, V> getChild(Object name) {
		return getChild(cache, name);
	}

	@Override
	public TreeNode<K, V> getChild(Object name, Flag... flags) {
		return getChild(cache.withFlags(flags), name);
	}

	private TreeNode<K, V> getChild(AdvancedCache cache, Object name) {
		startAtomic();
		try {
			if (hasChild(name))
				return new TreeNodeImpl<K, V>(Fqn.fromRelativeElements(fqn, name), cache, batchContainer);
			else
				return null;
		} finally {
			endAtomic();
		}
	}

	@Override
	public V put(K key, V value) {
		return put(cache, key, value);
	}

	@Override
	public V put(K key, V value, Flag... flags) {
		return put(cache.withFlags(flags), key, value);
	}

	private V put(AdvancedCache cache, K key, V value) {
		startAtomic();
		try {
			AtomicHashMapProxy<K, V> map = (AtomicHashMapProxy<K, V>) getDataInternal(cache);
			return map.put(key, value);
		} finally {
			endAtomic();
		}
	}

	@Override
	public V putIfAbsent(K key, V value) {
		return putIfAbsent(cache, key, value);
	}

	@Override
	public V putIfAbsent(K key, V value, Flag... flags) {
		return putIfAbsent(cache.withFlags(flags), key, value);
	}

	private V putIfAbsent(AdvancedCache<?, ?> cache, K key, V value) {
		startAtomic();
		try {
			AtomicMap<K, V> data = getDataInternal(cache);
			if (!data.containsKey(key)){
				return data.put(key, value);
			}
			return data.get(key);
		} finally {
			endAtomic();
		}
	}

	@Override
	public V replace(K key, V value) {
		return replace(cache, key, value);
	}

	@Override
	public V replace(K key, V value, Flag... flags) {
		return replace(cache.withFlags(flags), key, value);
	}

	private V replace(AdvancedCache<?, ?> cache, K key, V value) {
		startAtomic();
		try {
			AtomicMap<K, V> map = getAtomicMap(cache, dataKey);
			if (map.containsKey(key))
				return map.put(key, value);
			else
				return null;
		} finally {
			endAtomic();
		}
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue) {
		return replace(cache, key, oldValue, newValue);
	}

	@Override
	public boolean replace(K key, V oldValue, V newValue, Flag... flags) {
		return replace(cache.withFlags(flags), key, oldValue, newValue);
	}

	private boolean replace(AdvancedCache<?, ?> cache, K key, V oldValue, V newValue) {
		startAtomic();
		try {
			AtomicMap<K, V> data = getDataInternal(cache);
			V old = data.get(key);
			if (Util.safeEquals(oldValue, old)) {
				data.put(key, newValue);
				return true;
			}
			return false;
		} finally {
			endAtomic();
		}
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		putAll(cache, map);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map, Flag... flags) {
		putAll(cache.withFlags(flags), map);
	}

	private void putAll(AdvancedCache cache, Map<? extends K, ? extends V> map) {
		startAtomic();
		try {
			getDataInternal(cache).putAll(map);
		} finally {
			endAtomic();
		}
	}

	@Override
	public void replaceAll(Map<? extends K, ? extends V> map) {
		replaceAll(cache, map);
	}

	@Override
	public void replaceAll(Map<? extends K, ? extends V> map, Flag... flags) {
		replaceAll(cache.withFlags(flags), map);
	}

	private void replaceAll(AdvancedCache cache, Map<? extends K, ? extends V> map) {
		startAtomic();
		try {
			AtomicMap<K, V> data = getDataInternal(cache);
			data.clear();
			data.putAll(map);
		} finally {
			endAtomic();
		}
	}

	@Override
	public V get(K key) {
		return get(cache, key);
	}

	@Override
	public V get(K key, Flag... flags) {
		return get(cache.withFlags(flags), key);
	}

	private V get(AdvancedCache cache, K key) {
		return getData(cache).get(key);
	}

	@Override
	public V remove(K key) {
		return remove(cache, key);
	}

	@Override
	public V remove(K key, Flag... flags) {
		return remove(cache.withFlags(flags), key);
	}

	private V remove(AdvancedCache cache, K key) {
		startAtomic();
		try {
			return getDataInternal(cache).remove(key);
		} finally {
			endAtomic();
		}
	}

	@Override
	public void clearData() {
		clearData(cache);
	}

	@Override
	public void clearData(Flag... flags) {
		clearData(cache.withFlags(flags));
	}

	private void clearData(AdvancedCache<?, ?> cache) {
		getDataInternal(cache).clear();
	}

	@Override
	public int dataSize() {
		return dataSize(cache);
	}

	@Override
	public int dataSize(Flag... flags) {
		return dataSize(cache.withFlags(flags));
	}

	private int dataSize(AdvancedCache<?, ?> cache) {
		return getData(cache).size();
	}

	@Override
	public boolean hasChild(Fqn f) {
		return hasChild(cache, f);
	}

	@Override
	public boolean hasChild(Fqn f, Flag... flags) {
		return hasChild(cache.withFlags(flags), f);
	}

	private boolean hasChild(AdvancedCache<?, ?> cache, Fqn f) {
		if (f.size() > 1) {
			// indirect child.
			Fqn absoluteFqn = Fqn.fromRelativeFqn(fqn, f);
			return exists(cache, absoluteFqn);
		} else {
			return hasChild(f.getLastElement());
		}
	}

	@Override
	public boolean hasChild(Object o) {
		return hasChild(cache, o);
	}

	@Override
	public boolean hasChild(Object o, Flag... flags) {
		return hasChild(cache.withFlags(flags), o);
	}

	private boolean hasChild(AdvancedCache<?, ?> cache, Object o) {
		return getStructure(cache).containsKey(o);
	}

	@Override
	public boolean isValid() {
		return cache.containsKey(dataKey);
	}

	@Override
	public void removeChildren() {
		removeChildren(cache);
	}

	@Override
	public void removeChildren(Flag... flags) {
		removeChildren(cache.withFlags(flags));
	}

	public void begin() {
		super.startAtomic();
	}

	public void end() {
		super.endAtomic();
	}

	private void removeChildren(AdvancedCache<?, ?> cache) {
		startAtomic();
		try {
			Map<Object, Fqn> s = getStructure(cache);
			for (Object o : Immutables.immutableSetCopy(s.keySet()))
				removeChild(cache, o);
		} finally {
			endAtomic();
		}
	}

	AtomicMap<K, V> getDataInternal() {
		return getAtomicMap(dataKey);
	}

	AtomicMap<K, V> getDataInternal(AdvancedCache<?, ?> cache) {
		return getAtomicMap(cache, dataKey);
	}

	AtomicMap<Object, Fqn> getStructure() {
		return getAtomicMap(structureKey);
	}

	private AtomicMap<Object, Fqn> getStructure(AdvancedCache<?, ?> cache) {
		return getAtomicMap(cache, structureKey);
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		TreeNodeImpl<?, ?> node = (TreeNodeImpl<?, ?>) o;

		if (fqn != null ? !fqn.equals(node.fqn) : node.fqn != null)
			return false;

		return true;
	}

	public int hashCode() {
		return (fqn != null ? fqn.hashCode() : 0);
	}

	@Override
	public String toString() {
		return "TreeNode{" + "fqn=" + fqn + '}';
	}
}
