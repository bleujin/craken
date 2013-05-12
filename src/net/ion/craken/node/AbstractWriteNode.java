package net.ion.craken.node;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.ArrayUtils;

import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.tree.ExtendPropertyId;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;

import com.google.common.base.Optional;

public abstract class AbstractWriteNode implements WriteNode {

	private WriteSession wsession ;
	protected AbstractWriteNode(WriteSession wsession) {
		this.wsession = wsession ;
	}
	protected abstract TreeNode<PropertyId, PropertyValue> tree() ;
	protected abstract WriteNode load(WriteSession wsession, TreeNode<PropertyId, PropertyValue> inner) ;
	
	
	public WriteSession session(){
		return wsession ;
	}
	
	public String toString(){
		return this.getClass().getSimpleName() + "[fqn=" + fqn() + "]";
	}

	public WriteNode property(String key, Object value) {
		if (value.getClass().isArray()) {
			int length = Array.getLength(value);
			List list = ListUtil.newList() ;
			for (int i = 0; i < length; i++) {
	            list.add(Array.get(value, i));
	        }
			return append(key, list.toArray()) ;
		}
		return property(PropertyId.normal(key), PropertyValue.createPrimitive(value)) ;
	}
	
	private WriteNode property(PropertyId pid, PropertyValue pvalue){
		tree().put(pid, pvalue) ;
		return this ;
	}
	
	public WriteNode propertyIfAbsent(String key, Object value){
		tree().putIfAbsent(PropertyId.normal(key), PropertyValue.createPrimitive(value));
		return this ;
	}

	public PropertyValue propertyIfAbsentEnd(String key, Object value){
		return ObjectUtil.coalesce(tree().putIfAbsent(PropertyId.normal(key), PropertyValue.createPrimitive(value)), PropertyValue.NotFound) ;
	}
	

	public PropertyValue replace(String key, Object value){
		return ObjectUtil.coalesce(tree().replace(PropertyId.normal(key), PropertyValue.createPrimitive(value)), PropertyValue.NotFound)  ;
	}
	
	public boolean replace(String key, Object oldValue, Object newValue){
		return tree().replace(PropertyId.normal(key), PropertyValue.createPrimitive(oldValue), PropertyValue.createPrimitive(newValue)) ;
	}
	
	public WriteNode propertyAll(Map<String, ? extends Object> map){
		tree().putAll(modMap(map)) ;
		return this ;
	}
	
	private Map<PropertyId, PropertyValue> modMap(Map<String, ? extends Object> map) {
		Map<PropertyId, PropertyValue> modMap = MapUtil.newMap() ;
		for (Entry<String, ? extends Object> entry : map.entrySet()) {
			modMap.put(PropertyId.normal(entry.getKey()), PropertyValue.createPrimitive(entry.getValue())) ;
		}
		return modMap;
	}
	
	
	public WriteNode append(String key, Object... value){
		PropertyValue findValue = property(key) ;
		if (findValue == PropertyValue.NotFound) findValue = PropertyValue.createPrimitive(null) ;
		
		findValue.append(value) ;
		
		tree().put(PropertyId.normal(key), findValue) ;
		return this ;
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
		tree().clearData() ;
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
		return load(session(), last) ;
	}
	
	
	public boolean removeChild(String fqn){
		Fqn.fromRelativeFqn(fqn(), Fqn.fromString(fqn)).getLastElementAsString() ;
		return tree().removeChild(Fqn.fromRelativeFqn(fqn(), Fqn.fromString(fqn))) ;
	}
	
	public void removeChildren(){
		tree().removeChildren() ;
	}
	
	public boolean hasProperty(PropertyId pid){
		return keys().contains(pid) ;
	}

	
	public WriteNode ref(String refName) {
		PropertyId referId = PropertyId.refer(refName);
		if (hasProperty(referId)) {
			Object val = property(referId).value() ;
			if (val == null) new IllegalArgumentException("not found ref :" + refName) ;
			return wsession.pathBy(val.toString()) ;
		} else {
			throw new IllegalArgumentException("not found ref :" + refName) ;
		}
	}

	public IteratorList<WriteNode> refs(String refName){
		
		PropertyId referId = PropertyId.refer(refName);
		final Iterator<String> iter = hasProperty(referId) ? property(referId).asSet().iterator() : IteratorUtils.EMPTY_ITERATOR;
		
		return new IteratorList<WriteNode>() {
			@Override
			public List<WriteNode> toList() {
				List<WriteNode> result = ListUtil.newList() ;
				while(iter.hasNext()) {
					result.add(wsession.pathBy(iter.next())) ;
				}
				return Collections.unmodifiableList(result);
			}

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public WriteNode next() {
				return wsession.pathBy(iter.next());
			}
		};
	}
	
	public WriteNode fromJson(JsonObject json){
		for (Entry<String, JsonElement> entry : json.entrySet()) {
			append(this, entry.getKey(), entry.getValue()) ;
		} 
		
		return this;
	}
	
	private void append(WriteNode that, String propId, JsonElement json){
		if (json.isJsonNull()) {
			return ;
		} else if (json.isJsonPrimitive()){
			if (propId.startsWith("@")){
				that.refTos(propId.substring(1), json.getAsJsonPrimitive().getAsString()) ;
			} else {
				that.append(propId, json.getAsJsonPrimitive().getValue()) ;
			}
		} else if (json.isJsonArray()){
			for (JsonElement jele : json.getAsJsonArray().toArray()){
				append(that, propId, jele) ;
			}
		} else if (json.isJsonObject()){
			for (Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
				append(that.addChild(propId), entry.getKey(), entry.getValue()) ;
			}
		}
	}
	
	
	public WriteNode refTo(String refName, String fqn){
		PropertyId referId = PropertyId.refer(refName);
		tree().put(referId, PropertyValue.createPrimitive(fqn)) ;
		return this ;
	}
	

	public WriteNode refTos(String refName, String fqn){
		
		PropertyId referId = PropertyId.refer(refName);
		PropertyValue findValue = property(referId) ;
		if (findValue == PropertyValue.NotFound) findValue = PropertyValue.createPrimitive(null) ;
		
		findValue.append(fqn) ;
	
		
		tree().put(referId, findValue) ;
		return this ;
	}
	
	public boolean removeSelf(){
		return parent().removeChild(fqn().name()) ;
	}
	
	// common
	public Fqn fqn(){
		return tree().getFqn() ;
	}
	
	public int dataSize(){
		return tree().dataSize() ;
	}
	
	public WriteNode parent(){
		return load(session(), tree().getParent()) ;
	}
	
	
	public boolean hasChild(String fqn){
		return tree().hasChild(Fqn.fromString(fqn)) ;
	}
	
	public WriteNode child(String fqn){
		return wsession.pathBy(Fqn.fromRelativeFqn(this.fqn(), Fqn.fromString(fqn))) ;
//		return load(wsession(), tree().getChild(Fqn.fromString(fqn))) ;
	}
	
	public WriteNode root(){
		return wsession.root() ;
	}

	
	public Set<String> childrenNames(){
		Set<String> set = SetUtil.orderedSet(SetUtil.newSet());
		for (Object object : tree().getChildrenNames()) {
			set.add(ObjectUtil.toString(object)) ;
		}
		return set ;
	}
	
	public Set<PropertyId> keys(){
		return tree().getKeys() ;
	}
	
	public PropertyValue property(String key) {
		return property(PropertyId.normal(key));
	}
	
	public boolean hasRef(String refName){
		return keys().contains(PropertyId.refer(refName)) ;
	}
	
	public boolean hasRef(String refName, Fqn fqn){
		return property(PropertyId.refer(refName)).asSet().contains(fqn.toString()) ;
	}
	
	public PropertyValue extendProperty(String propPath) {
		return ExtendPropertyId.create(propPath).propValue(this) ;
	}
	
	public PropertyValue property(PropertyId pid) {
		return ObjectUtil.coalesce(tree().get(pid), PropertyValue.NotFound);
	}
	
	public Map<PropertyId, PropertyValue> toMap() {
		return Collections.unmodifiableMap(tree().getData());
	}
	
	public Object id(){
		return fqn() ;
	}
}
