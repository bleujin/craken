package net.ion.craken.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.ion.craken.io.GridFilesystem;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.AtomicHashMapProxy;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.batch.BatchContainer;
import org.infinispan.context.Flag;
import org.infinispan.util.Immutables;
import org.infinispan.util.Util;

public class TreeNodeImpl extends TreeStructureSupport implements TreeNode<PropertyId, PropertyValue> {
	private GridFilesystem gfs ;
	private Fqn fqn;
	private TreeNodeKey dataKey, structureKey;

	public TreeNodeImpl(Fqn fqn, GridFilesystem gfs, AdvancedCache<?, ?> cache, BatchContainer batchContainer) {
		super(cache, batchContainer);
		this.gfs = gfs ;
		this.fqn = fqn;
		this.dataKey = new TreeNodeKey(fqn, TreeNodeKey.Type.DATA);
		this.structureKey = new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE);
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> getParent() {
		return getParent(cache);
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> getParent(Flag... flags) {
		return getParent(cache.withFlags(flags));
	}

	private TreeNode<PropertyId, PropertyValue> getParent(AdvancedCache<?, ?> cache) {
		if (fqn.isRoot())
			return this;
		return new TreeNodeImpl(fqn.getParent(), gfs, cache, batchContainer);
	}

	@Override
	public Set<TreeNode<PropertyId, PropertyValue>> getChildren() {
		return getChildren(cache);
	}

	@Override
	public Set<TreeNode<PropertyId, PropertyValue>> getChildren(Flag... flags) {
		return getChildren(cache.withFlags(flags));
	}

	private Set<TreeNode<PropertyId, PropertyValue>> getChildren(AdvancedCache<?, ?> cache) {
		startAtomic();
		try {
			Set<TreeNode<PropertyId, PropertyValue>> result = new HashSet<TreeNode<PropertyId, PropertyValue>>();
			for (Fqn f : getStructure().values()) {
				TreeNodeImpl n = new TreeNodeImpl(f, gfs, cache, batchContainer);
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
	public Map<PropertyId, PropertyValue> getData() {
		return getData(cache);
	}

	@Override
	public Map<PropertyId, PropertyValue> getData(Flag... flags) {
		return getData(cache.withFlags(flags));
	}

	private Map<PropertyId, PropertyValue> getData(AdvancedCache<?, ?> cache) {
		return Collections.unmodifiableMap(new ReadMap(gfs, getDataInternal(cache)));
	}

	@Override
	public Set<PropertyId> getKeys() {
		return getKeys(cache);
	}

	@Override
	public Set<PropertyId> getKeys(Flag... flags) {
		return getKeys(cache.withFlags(flags));
	}

	private Set<PropertyId> getKeys(AdvancedCache<?, ?> cache) {
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
	public TreeNode<PropertyId, PropertyValue> addChild(Fqn f) {
		return addChild(cache, f);
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> addChild(Fqn f, Flag... flags) {
		return addChild(cache.withFlags(flags), f);
	}

	private TreeNode<PropertyId, PropertyValue> addChild(AdvancedCache<?, ?> cache, Fqn f) {
		startAtomic();
		try {
			Fqn absoluteChildFqn = Fqn.fromRelativeFqn(fqn, f);

			// 1) first register it with the parent
			AtomicMap<Object, Fqn> structureMap = getStructure(cache);
			structureMap.put(f.getLastElement(), absoluteChildFqn);

			// 2) then create the structure and data maps
			createNodeInCache(cache, absoluteChildFqn);

			final TreeNodeImpl result = new TreeNodeImpl(absoluteChildFqn, gfs, cache, batchContainer);

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
		AtomicMap<Object, Fqn> s = getStructure(cache);
		Fqn childFqn = s.remove(childName);
		if (childFqn != null) {
			TreeNode<PropertyId, PropertyValue> child = new TreeNodeImpl(childFqn, gfs, cache, batchContainer);
			child.removeChildren();
			child.clearData(); // this is necessary in case we have a remove and then an add on the same node, in the same tx.
			cache.remove(new TreeNodeKey(childFqn, TreeNodeKey.Type.DATA));
			cache.remove(new TreeNodeKey(childFqn, TreeNodeKey.Type.STRUCTURE));
			return true;
		}

		return false;
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> getChild(Fqn f) {
		return getChild(cache, f);
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> getChild(Fqn f, Flag... flags) {
		return getChild(cache.withFlags(flags), f);
	}

	private TreeNode<PropertyId, PropertyValue> getChild(AdvancedCache cache, Fqn f) {
		startAtomic();
		try {
			if (hasChild(f))
				return new TreeNodeImpl(Fqn.fromRelativeFqn(fqn, f), gfs, cache, batchContainer);
			else
				return null;
		} finally {
			endAtomic();
		}
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> getChild(Object name) {
		return getChild(cache, name);
	}

	@Override
	public TreeNode<PropertyId, PropertyValue> getChild(Object name, Flag... flags) {
		return getChild(cache.withFlags(flags), name);
	}

	private TreeNode<PropertyId, PropertyValue> getChild(AdvancedCache cache, Object name) {
		startAtomic();
		try {
			if (hasChild(name))
				return new TreeNodeImpl(Fqn.fromRelativeElements(fqn, name), gfs, cache, batchContainer);
			else
				return null;
		} finally {
			endAtomic();
		}
	}

	@Override
	public PropertyValue put(PropertyId key, PropertyValue value) {
		return put(cache, key, value);
	}

	@Override
	public PropertyValue put(PropertyId key, PropertyValue value, Flag... flags) {
		return put(cache.withFlags(flags), key, value);
	}

	private PropertyValue put(AdvancedCache cache, PropertyId key, PropertyValue value) {

		AtomicHashMapProxy<PropertyId, PropertyValue> map = (AtomicHashMapProxy<PropertyId, PropertyValue>) getDataInternal(cache);
		return map.put(key, value);

	}

	@Override
	public PropertyValue putIfAbsent(PropertyId key, PropertyValue value) {
		return putIfAbsent(cache, key, value);
	}

	@Override
	public PropertyValue putIfAbsent(PropertyId key, PropertyValue value, Flag... flags) {
		return putIfAbsent(cache.withFlags(flags), key, value);
	}

	private PropertyValue putIfAbsent(AdvancedCache<?, ?> cache, PropertyId key, PropertyValue value) {
		AtomicMap<PropertyId, PropertyValue> data = getDataInternal(cache);
		if (!data.containsKey(key)) {
			return data.put(key, value);
		}
		return data.get(key);
	}

	@Override
	public PropertyValue replace(PropertyId key, PropertyValue value) {
		return replace(cache, key, value);
	}

	@Override
	public PropertyValue replace(PropertyId key, PropertyValue value, Flag... flags) {
		return replace(cache.withFlags(flags), key, value);
	}

	private PropertyValue replace(AdvancedCache<?, ?> cache, PropertyId key, PropertyValue value) {
		AtomicMap<PropertyId, PropertyValue> map = getAtomicMap(cache, dataKey);
		if (map.containsKey(key))
			return map.put(key, value);
		else
			return null;
	}

	@Override
	public boolean replace(PropertyId key, PropertyValue oldValue, PropertyValue newValue) {
		return replace(cache, key, oldValue, newValue);
	}

	@Override
	public boolean replace(PropertyId key, PropertyValue oldValue, PropertyValue newValue, Flag... flags) {
		return replace(cache.withFlags(flags), key, oldValue, newValue);
	}

	private boolean replace(AdvancedCache<?, ?> cache, PropertyId key, PropertyValue oldValue, PropertyValue newValue) {
		AtomicMap<PropertyId, PropertyValue> data = getDataInternal(cache);
		PropertyValue old = data.get(key);
		if (Util.safeEquals(oldValue, old)) {
			data.put(key, newValue);
			return true;
		}
		return false;
	}

	@Override
	public void putAll(Map<? extends PropertyId, ? extends PropertyValue> map) {
		putAll(cache, map);
	}

	@Override
	public void putAll(Map<? extends PropertyId, ? extends PropertyValue> map, Flag... flags) {
		putAll(cache.withFlags(flags), map);
	}

	private void putAll(AdvancedCache cache, Map<? extends PropertyId, ? extends PropertyValue> map) {
		getDataInternal(cache).putAll(map);
	}

	@Override
	public void replaceAll(Map<? extends PropertyId, ? extends PropertyValue> map) {
		replaceAll(cache, map);
	}

	@Override
	public void replaceAll(Map<? extends PropertyId, ? extends PropertyValue> map, Flag... flags) {
		replaceAll(cache.withFlags(flags), map);
	}

	private void replaceAll(AdvancedCache cache, Map<? extends PropertyId, ? extends PropertyValue> map) {
		AtomicMap<PropertyId, PropertyValue> data = getDataInternal(cache);
		data.clear();
		data.putAll(map);
	}

	@Override
	public PropertyValue get(PropertyId key) {
		return get(cache, key);
	}

	@Override
	public PropertyValue get(PropertyId key, Flag... flags) {
		return get(cache.withFlags(flags), key);
	}

	private PropertyValue get(AdvancedCache cache, PropertyId key) {
		return getData(cache).get(key);
	}

	@Override
	public PropertyValue remove(PropertyId key) {
		return remove(cache, key);
	}

	@Override
	public PropertyValue remove(PropertyId key, Flag... flags) {
		return remove(cache.withFlags(flags), key);
	}

	private PropertyValue remove(AdvancedCache cache, PropertyId key) {
		return getDataInternal(cache).remove(key);
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
		Map<Object, Fqn> s = getStructure(cache);
		for (Object o : Immutables.immutableSetCopy(s.keySet()))
			removeChild(cache, o);
	}

	AtomicMap<PropertyId, PropertyValue> getDataInternal() {
		return getAtomicMap(dataKey);
	}

	AtomicMap<PropertyId, PropertyValue> getDataInternal(AdvancedCache<?, ?> cache) {
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

		TreeNodeImpl node = (TreeNodeImpl) o;

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


class ReadMap implements Map<PropertyId, PropertyValue>{

	private final GridFilesystem gfs ;
	private final Map<PropertyId, PropertyValue> internal ; 
	public ReadMap(GridFilesystem gfs, AtomicMap<PropertyId, PropertyValue> internal) {
		this.gfs = gfs ;
		this.internal = internal ;
	}

	@Override
	public void clear() {
		internal.clear() ;
	}

	@Override
	public boolean containsKey(Object key) {
		return internal.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return internal.containsValue(value);
	}

	@Override
	public boolean isEmpty() {
		return internal.isEmpty();
	}

	@Override
	public Set<PropertyId> keySet() {
		return internal.keySet();
	}

	@Override
	public int size() {
		return internal.size();
	}

	
	

	@Override
	public Set<java.util.Map.Entry<PropertyId, PropertyValue>> entrySet() {
		for (PropertyValue pvalue : internal.values()) {
			pvalue.gfs(gfs) ;
		}
		return internal.entrySet() ;
	}

	@Override
	public PropertyValue get(Object key) {
		final PropertyValue value = ObjectUtil.coalesce(internal.get(key), PropertyValue.NotFound);
		return value.gfs(gfs);
	}

	@Override
	public Collection<PropertyValue> values() {
		for (PropertyValue pv : internal.values()) {
			pv.gfs(gfs) ;
		}
		return internal.values();
	}
	
	

	@Override
	public PropertyValue put(PropertyId key, PropertyValue value) {
		return null;
	}

	@Override
	public void putAll(Map<? extends PropertyId, ? extends PropertyValue> m) {
		
	}

	@Override
	public PropertyValue remove(Object key) {
		return null;
	}

	
}

