package net.ion.craken.node;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.SetUtil;

import com.google.common.base.Optional;

public abstract class AbstractWriteNode implements WriteNode {

	protected abstract TreeNode<PropertyId, PropertyValue> tree() ;
	protected abstract WriteNode load(TreeNode<PropertyId, PropertyValue> inner) ;
	
	public String toString(){
		return this.getClass().getSimpleName() + "[fqn=" + fqn() + "]";
	}

	public WriteNode property(String key, Object value) {
		tree().put(PropertyId.normal(key), PropertyValue.primitive(value)) ;
		return this ;
	}
	
	public Object propertyIfAbsent(String key, Object value){
		return tree().putIfAbsent(PropertyId.normal(key), PropertyValue.primitive(value)) ;
	}
	
	public Object replace(String key, Object value){
		return tree().replace(PropertyId.normal(key), PropertyValue.primitive(value)) ;
	}
	
	public boolean replace(String key, Object oldValue, Object newValue){
		return tree().replace(PropertyId.normal(key), PropertyValue.primitive(oldValue), PropertyValue.primitive(newValue)) ;
	}
	
	public WriteNode propertyAll(Map<String, ? extends Object> map){
		tree().putAll(modMap(map)) ;
		return this ;
	}
	private Map<PropertyId, PropertyValue> modMap(Map<String, ? extends Object> map) {
		Map<PropertyId, PropertyValue> modMap = MapUtil.newMap() ;
		for (Entry<String, ? extends Object> entry : map.entrySet()) {
			modMap.put(PropertyId.normal(entry.getKey()), PropertyValue.primitive(entry.getValue())) ;
		}
		return modMap;
	}
	
	public WriteNode replaceAll(Map<String, ? extends Object> newMap){
		tree().replaceAll(modMap(newMap)) ;
		return this ;
	}
	
	
	public WriteNode unset(String key){
		tree().remove(PropertyId.normal(key)) ;
		return this ;
	}
	
	
	public WriteNode clear(){
//		Object id = id() ;
		tree().clearData() ;
//		inner().put(NodeCommon.IDProp, id) ;
		return this ;
	}
	
	
	public WriteNode addChild(String relativeFqn){
//		final TreeNode find = inner.addChild(Fqn.fromString(relativeFqn));
//		return load(find) ;
		
		
		Iterator<Object> iter = Fqn.fromString(relativeFqn).peekElements().iterator();
		
		TreeNode last = tree() ;
		while(iter.hasNext()){
			last = last.addChild(Fqn.fromElements(iter.next()));
		}
		return load(last) ;
	}
	
	
	public boolean removeChild(String fqn){
		return tree().removeChild(Fqn.fromString(fqn)) ;
	}
	
	public void removeChildren(){
		tree().removeChildren() ;
	}
	
	
	public WriteNode refTo(String refName, String fqn, boolean isFirst){
		List<String> list = (List<String>) property(refName) ;
		if (list == null || list.size() == 0) {
			list = ListUtil.<String>newList();
		}
		if (isFirst) list.add(0, fqn) ; 
		else list.add(fqn) ;
		
		return property(refName, list) ;
	}
	
	
	public WriteNode refToLast(String refName, String fqn){
		return refTo(refName, fqn.toString(), false) ;
	}
	public WriteNode refToFirst(String refName, String fqn){
		return refTo(refName, fqn.toString(), true) ;
	}
	
	
	// common
	public Fqn fqn(){
		return tree().getFqn() ;
	}
	
	public int dataSize(){
		return tree().dataSize() ;
	}
	
	public WriteNode parent(){
		return load(tree().getParent()) ;
	}
	
	
	public boolean hasChild(String fqn){
		return tree().hasChild(Fqn.fromString(fqn)) ;
	}
	
	public WriteNode child(String fqn){
		return load(tree().getChild(Fqn.fromString(fqn))) ;
	}
	
	public Set<Object> childrenNames(){
		return tree().getChildrenNames() ;
	}
		
	public Set<PropertyId> keys(){
		return tree().getKeys() ;
	}
	
	public PropertyValue property(String key) {
		return tree().get(PropertyId.normal(key));
	}

	public Optional optional(String key) {
		return Optional.fromNullable(tree().get(PropertyId.normal(key)));
	}
	
	
	
	public Object id(){
		return fqn() ;
	}
}
