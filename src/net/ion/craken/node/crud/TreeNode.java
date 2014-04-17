package net.ion.craken.node.crud;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.ion.craken.io.GridFilesystem;
import net.ion.craken.node.Workspace;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;

import org.apache.commons.collections.map.HashedMap;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.util.Immutables;
import org.infinispan.util.Util;

import com.sun.corba.se.spi.orbutil.threadpool.Work;


public class TreeNode {

	private Workspace workspace ;
	private Fqn fqn;
	private AtomicMap<PropertyId, PropertyValue> lazyProp = null ;
	private AtomicMap<String, Fqn> lazyStru = null ;

	protected TreeNode(Workspace workspace, Fqn fqn) {
		this.workspace = workspace ;
		this.fqn = fqn;
	}
	
	public final static TreeNode create(Workspace workspace, Fqn fqn){
		return new TreeNode(workspace, fqn) ;
	} 

	private synchronized AtomicMap<PropertyId, PropertyValue> props() {
		if (lazyProp == null){
			this.lazyProp = workspace.props(fqn);
		}
		return this.lazyProp ;
	}
	
	private synchronized Map<String, Fqn> strus() {
		if (lazyStru == null){
			this.lazyStru = workspace.strus(fqn) ;
		}
		
		return lazyStru;
	}
	
	
	public TreeNode parent() {
		if (fqn.isRoot())
			return this;
		return new TreeNode(workspace, fqn.getParent());
	}

	public Fqn fqn() {
		return fqn;
	}
	

	public Set<PropertyId> keys() {
		return props().keySet();
	}

	public Map<PropertyId, PropertyValue> readMap() {
		return new HashedMap(props());
	}
	
	public JsonObject toValueJson(){
		JsonObject result = new JsonObject();
		for (Entry<PropertyId, PropertyValue> prop : props().entrySet()) {
			result.add(prop.getKey().idString(), prop.getValue().json()); 
		}
		
		return result ;
	}
	

	public Set<TreeNode> getChildren() {
		Set<TreeNode> result = SetUtil.newOrdereddSet();
		for (Fqn f : strus().values()) {
			result.add(new TreeNode(workspace, f));
		}
		return Immutables.immutableSetWrap(result);
	}

	public Set<String> getChildrenNames() {
		return Immutables.immutableSetCopy(strus().keySet());
	}

	
	public Set<TreeNode> getReferences(String refName){
		PropertyValue pvalue = this.get(PropertyId.refer(refName)) ;
		if (pvalue == null) return SetUtil.EMPTY ;
		
		Set<TreeNode> result = SetUtil.newOrdereddSet();
		String[] refs = pvalue.asStrings() ;
		for (String refPath : refs) {
			Fqn refFqn = Fqn.fromString(refPath) ;
			if (! workspace.exists(refFqn)) continue ;
			result.add(new TreeNode(workspace, refFqn));
		}
		return Immutables.immutableSetWrap(result);
	}

//	public Map<String, Fqn> removeChild(Fqn f) {
//		return removeChild(f.getLastElement());
//	}

	public Map<String, Fqn> removeChild(String childName) {
		Map<String, Fqn> s = strus();
		Fqn childFqn = s.remove(childName);
		if (childFqn != null) {
			Map<String, Fqn> result = MapUtil.newMap() ;
			result.put(childFqn.toString(), childFqn) ;
			
			TreeNode child = new TreeNode(workspace, childFqn);
			result.putAll(child.removeChildren());
			child.clearData(); // this is necessary in case we have a remove and then an add on the same node, in the same tx.
			workspace.remove(childFqn);
			
			return result;
		}

		return MapUtil.EMPTY;
	}
	
	public TreeNode getChild(Fqn f) {
		if (hasChild(f))
			return new TreeNode(workspace, Fqn.fromRelativeFqn(fqn, f));
		else
			return null;
	}

	public PropertyValue put(PropertyId key, PropertyValue value) {
		Map<PropertyId, PropertyValue> map = props();
		return map.put(key, value);
	}

	public PropertyValue putIfAbsent(PropertyId key, PropertyValue value) {
		Map<PropertyId, PropertyValue> data = props() ;
		if (!data.containsKey(key)) {
			return data.put(key, value);
		}
		return data.get(key);
	}

	public PropertyValue replace(PropertyId key, PropertyValue value) {
		Map<PropertyId, PropertyValue> map = props() ;
		if (map.containsKey(key))
			return map.put(key, value);
		else
			return null;
	}

	public boolean replace(PropertyId key, PropertyValue oldValue, PropertyValue newValue) {
		Map<PropertyId, PropertyValue> data = props();
		PropertyValue old = data.get(key);
		if (Util.safeEquals(oldValue, old)) {
			data.put(key, newValue);
			return true;
		}
		return false;
	}

	public void putAll(Map<? extends PropertyId, ? extends PropertyValue> map) {
		props().putAll(map);
	}

	public PropertyValue get(PropertyId key) {
		return props().get(key);
	}

	public PropertyValue remove(PropertyId key) {
		return props().remove(key);
	}
	public void clearData() {
		props().clear();
	}

	public int dataSize() {
		return props().size();
	}

	public boolean hasChild(Fqn f) {
		if (f.size() > 1) {
			// indirect child.
			Fqn absoluteFqn = Fqn.fromRelativeFqn(fqn, f);
			return workspace.exists(absoluteFqn) ;
		} else {
			return hasChild(f.getLastElement());
		}
	}

	public boolean hasChild(Object o) {
		return strus().containsKey(o);
	}

	public Map<String, Fqn> removeChildren() {
		Map<String, Fqn> result = MapUtil.newMap() ;

		Map<String, Fqn> s = strus();
		for (Fqn rfqn : s.values()) {
			result.put(rfqn.toString(), rfqn) ;
		}
		
		for (String o : Immutables.immutableSetCopy(s.keySet()))
			result.putAll(removeChild(o));
		
		return result ;
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

	private GridFilesystem gfs ;
	private final Map<PropertyId, PropertyValue> internal ; 
	public ReadMap(GridFilesystem gfs, Map<PropertyId, PropertyValue> internal) {
		this.gfs = gfs ;
		this.internal = internal ;
		for (PropertyValue pvalue : internal.values()) {
			pvalue.gfs(gfs) ;
		}		
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
//		for (PropertyValue pvalue : internal.values()) {
//			pvalue.gfs(gfs) ;
//		}
		return internal.entrySet() ;
	}

	@Override
	public PropertyValue get(Object key) {
		final PropertyValue value = ObjectUtil.coalesce(internal.get(key), PropertyValue.NotFound);
		return value.gfs(gfs);
	}

	@Override
	public Collection<PropertyValue> values() {
//		for (PropertyValue pv : internal.values()) {
//			pv.gfs(gfs) ;
//		}
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
