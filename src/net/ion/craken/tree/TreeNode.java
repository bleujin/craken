package net.ion.craken.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.ion.craken.io.GridFilesystem;
import net.ion.framework.util.ObjectUtil;

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.AtomicHashMapProxy;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.batch.BatchContainer;
import org.infinispan.util.Immutables;
import org.infinispan.util.Util;


public class TreeNode extends TreeStructureSupport {

	private GridFilesystem gfs ;
	private Fqn fqn;
	private TreeNodeKey dataKey, structureKey;

	public TreeNode(Fqn fqn, GridFilesystem gfs, AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, BatchContainer batchContainer) {
		super(cache, batchContainer) ;
		this.gfs = gfs ;
		this.fqn = fqn;
		this.dataKey = new TreeNodeKey(fqn, TreeNodeKey.Type.DATA);
		this.structureKey = new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE);
		this.batchContainer = batchContainer ;
	}

	public TreeNode getParent() {
		if (fqn.isRoot())
			return this;
		return new TreeNode(fqn.getParent(), gfs, cache, batchContainer);
	}

	public Set<TreeNode> getChildren() {
		Set<TreeNode> result = new HashSet<TreeNode>();
		for (Fqn f : getStructure(structureKey).values()) {
			result.add(new TreeNode(f, gfs, cache, batchContainer));
		}
		return Immutables.immutableSetWrap(result);
	}

	public Set<Object> getChildrenNames() {
		return Immutables.immutableSetCopy(getStructure(structureKey).keySet());
	}

	public Map<PropertyId, PropertyValue> getData() {
		return Collections.unmodifiableMap(new ReadMap(gfs, getDataInternal()));
	}

	public Set<PropertyId> getKeys() {
		return getData().keySet();
	}

	public Fqn getFqn() {
		return fqn;
	}

	public TreeNode addChild(Fqn f) {
		Fqn absoluteChildFqn = Fqn.fromRelativeFqn(fqn, f);

		// 1) first register it with the parent
		// AtomicMap<Object, Fqn> structureMap = getStructure(structureKey);
		// structureMap.put(f.getLastElement(), absoluteChildFqn);

		// Debug.line(f, new TreeNodeKey(absoluteChildFqn.getParent() , TreeNodeKey.Type.STRUCTURE)) ;
		// cache.remove(new TreeNodeKey(absoluteChildFqn.getParent() , TreeNodeKey.Type.STRUCTURE));

		// 2) then create the structure and data maps
		mergeAncestor(absoluteChildFqn);

		return new TreeNode(absoluteChildFqn, gfs, cache, batchContainer);
	}

	public boolean removeChild(Fqn f) {
		return removeChild(f.getLastElement());
	}

	public boolean removeChild(Object childName) {
		AtomicMap<Object, Fqn> s = getStructure(structureKey);
		Fqn childFqn = s.remove(childName);
		if (childFqn != null) {
			TreeNode child = new TreeNode(childFqn, gfs, cache, batchContainer);
			child.removeChildren();
			child.clearData(); // this is necessary in case we have a remove and then an add on the same node, in the same tx.
			cache.remove(new TreeNodeKey(childFqn, TreeNodeKey.Type.DATA));
			cache.remove(new TreeNodeKey(childFqn, TreeNodeKey.Type.STRUCTURE));
			return true;
		}

		return false;
	}
	
	public TreeNode getChild(Fqn f) {
		if (hasChild(f))
			return new TreeNode(Fqn.fromRelativeFqn(fqn, f), gfs, cache, batchContainer);
		else
			return null;
	}

	public PropertyValue put(PropertyId key, PropertyValue value) {

		AtomicHashMapProxy<PropertyId, PropertyValue> map = (AtomicHashMapProxy<PropertyId, PropertyValue>) getDataInternal();
		return map.put(key, value);
	}

	public PropertyValue putIfAbsent(PropertyId key, PropertyValue value) {
		AtomicMap<PropertyId, PropertyValue> data = getDataInternal();
		if (!data.containsKey(key)) {
			return data.put(key, value);
		}
		return data.get(key);
	}

	public PropertyValue replace(PropertyId key, PropertyValue value) {
		AtomicMap<PropertyId, PropertyValue> map = getAtomicMap(dataKey);
		if (map.containsKey(key))
			return map.put(key, value);
		else
			return null;
	}

	public boolean replace(PropertyId key, PropertyValue oldValue, PropertyValue newValue) {
		AtomicMap<PropertyId, PropertyValue> data = getDataInternal();
		PropertyValue old = data.get(key);
		if (Util.safeEquals(oldValue, old)) {
			data.put(key, newValue);
			return true;
		}
		return false;
	}

	public void putAll(Map<? extends PropertyId, ? extends PropertyValue> map) {
		getDataInternal().putAll(map);
	}

	public void replaceAll(Map<? extends PropertyId, ? extends PropertyValue> map) {
		AtomicMap<PropertyId, PropertyValue> data = getDataInternal();
		data.clear();
		data.putAll(map);
	}

	public PropertyValue get(PropertyId key) {
		return getData().get(key);
	}

	public PropertyValue remove(PropertyId key) {
		return getDataInternal().remove(key);
	}
	public void clearData() {
		getDataInternal().clear();
	}

	public int dataSize() {
		return getData().size();
	}

	public boolean hasChild(Fqn f) {
		if (f.size() > 1) {
			// indirect child.
			Fqn absoluteFqn = Fqn.fromRelativeFqn(fqn, f);
			return exists(absoluteFqn);
		} else {
			return hasChild(f.getLastElement());
		}
	}

	public boolean hasChild(Object o) {
		return getStructure(structureKey).containsKey(o);
	}

	public boolean isValid() {
		return cache.containsKey(dataKey);
	}

	public void removeChildren() {
		Map<Object, Fqn> s = getStructure(structureKey);
		for (Object o : Immutables.immutableSetCopy(s.keySet()))
			removeChild(o);
	}
	
	AtomicMap<PropertyId, PropertyValue> getDataInternal() {
		return getAtomicMap(dataKey);
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		TreeNode node = (TreeNode) o;

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
		throw new UnsupportedOperationException() ;
	}

	@Override
	public void putAll(Map<? extends PropertyId, ? extends PropertyValue> m) {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public PropertyValue remove(Object key) {
		throw new UnsupportedOperationException() ;
	}


}
