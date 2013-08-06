package net.ion.craken.node.crud;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.ion.craken.io.BlobProxy;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.exception.NodeIOException;
import net.ion.craken.tree.ExtendPropertyId;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;

import org.apache.commons.collections.IteratorUtils;
import org.apache.lucene.analysis.kr.utils.StringUtil;

import com.google.common.base.Function;

public class WriteNodeImpl implements WriteNode{


	private WriteSession wsession ;
	private TreeNode inner ;
	
	public enum Touch {
		MODIFY, REMOVE, REMOVECHILDREN
	}
	
	private WriteNodeImpl(WriteSession wsession, TreeNode inner) {
		this.wsession = wsession ;
		this.inner = inner ;
	}
	
	public static WriteNode loadTo(WriteSession wsession, TreeNode inner) {
		return new WriteNodeImpl(wsession, inner);
	}

	public WriteNode load(WriteSession wsession, TreeNode inner) {
		return new WriteNodeImpl(wsession, inner);
	}
	
	protected TreeNode tree(){
		return inner ;
	}


	public WriteSession session(){
		return wsession ;
	}

	
	private PropertyId createNormalId(String key){
		return wsession.idInfoTo(PropertyId.normal(key)) ; 
	}
	
	private PropertyId createReferId(String key){
		return wsession.idInfoTo(PropertyId.refer(key)) ;
	}
	
	
	
	
	
	

	public WriteNode property(String key, Object value) {
		if (value != null && value.getClass().isArray()) {
			int length = Array.getLength(value);
			List list = ListUtil.newList() ;
			for (int i = 0; i < length; i++) {
	            list.add(Array.get(value, i));
	        }
			return append(key, list.toArray()) ;
		}
		return property(createNormalId(key), PropertyValue.createPrimitive(value)) ;
	}
	
	
	
	private WriteNode property(PropertyId pid, PropertyValue pvalue){
		touch(Touch.MODIFY) ;
		
		tree().put(pid, pvalue) ;
		return this ;
	}
	
	public WriteNode propertyIfAbsent(String key, Object value){
		touch(Touch.MODIFY) ;
		
		tree().putIfAbsent(createNormalId(key), PropertyValue.createPrimitive(value));
		return this ;
	}

	public PropertyValue propertyIfAbsentEnd(String key, Object value){
		touch(Touch.MODIFY) ;
		
		return ObjectUtil.coalesce(tree().putIfAbsent(createNormalId(key), PropertyValue.createPrimitive(value)), PropertyValue.NotFound) ;
	}
	

	public PropertyValue replace(String key, Object value){
		touch(Touch.MODIFY) ;
		return ObjectUtil.coalesce(tree().replace(createNormalId(key), PropertyValue.createPrimitive(value)), PropertyValue.NotFound)  ;
	}
	
	public boolean replace(String key, Object oldValue, Object newValue){
		touch(Touch.MODIFY) ;
		return tree().replace(createNormalId(key), PropertyValue.createPrimitive(oldValue), PropertyValue.createPrimitive(newValue)) ;
	}
	
	public WriteNode propertyAll(Map<String, ? extends Object> map){
		touch(Touch.MODIFY) ;

		tree().putAll(modMap(map)) ;
		return this ;
	}

	
	public WriteNode append(String key, Object... value){
		touch(Touch.MODIFY) ;
		PropertyValue findValue = property(key) ;
		if (findValue == PropertyValue.NotFound) findValue = PropertyValue.createPrimitive(null) ;
		
		findValue.append(value) ;
		
		tree().put(createNormalId(key), findValue) ;
		return this ;
	}
	

	public WriteNode replaceAll(Map<String, ? extends Object> newMap){
		touch(Touch.MODIFY) ;
		tree().replaceAll(modMap(newMap)) ;
		return this ;
	}
	
	
	public WriteNode unset(String key){
		touch(Touch.MODIFY) ;
		tree().remove(createNormalId(key)) ;
		return this ;
	}

	
	public WriteNode clear(){
		touch(Touch.MODIFY) ;
		tree().clearData() ;
		return this ;
	}
	
	
	public WriteNode blob(String key, File file) {
		try {
			return blob(key, new FileInputStream(file));
		} catch (FileNotFoundException e) {
			throw new NodeIOException(e) ;
		}
	}

	public WriteNode blob(String key, InputStream input) {
		try {
			BlobProxy blobValue = wsession.workspace().blob(wsession.workspace().wsName() + fqn().toString() + "." + key, input);
			property(key, blobValue) ;
		} catch (IOException e) {
			throw new NodeIOException(e) ;
		} finally {
			IOUtil.closeSilent(input) ;
		}
		
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
		final Fqn target = Fqn.fromRelativeFqn(fqn(), Fqn.fromString(fqn));
		touch(Touch.REMOVE, target) ;
		return tree().removeChild(target) ;
	}
	
	public void removeChildren(){
		touch(Touch.REMOVECHILDREN) ;
		tree().removeChildren() ;
	}
	
	public boolean hasProperty(PropertyId pid){
		return keys().contains(pid) ;
	}

	public WriteNode ref(String refName) {
		PropertyId referId = createReferId(refName);
		if (hasProperty(referId)) {
			Object val = property(referId).value() ;
			if (val == null) new IllegalArgumentException("not found ref :" + refName) ;
			return wsession.pathBy(val.toString()) ;
		} else {
			throw new IllegalArgumentException("not found ref :" + refName) ;
		}
	}

	public IteratorList<WriteNode> refs(String refName){
		
		PropertyId referId = createReferId(refName);
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
	
	private Map<PropertyId, PropertyValue> modMap(Map<String, ? extends Object> map) {
		Map<PropertyId, PropertyValue> modMap = MapUtil.newMap() ;
		for (Entry<String, ? extends Object> entry : map.entrySet()) {
			modMap.put(createNormalId(entry.getKey()), PropertyValue.createPrimitive(entry.getValue())) ;
		}
		return modMap;
	}
	
	
	public WriteNode refTo(String refName, String fqn){
		PropertyId referId = createReferId(refName);
		if (StringUtil.isBlank(fqn)) tree().remove(referId) ;
		else tree().put(referId, PropertyValue.createPrimitive(fqn)) ;

		touch(Touch.MODIFY) ;
		return this ;
	}
	

	public WriteNode refTos(String refName, String fqn){
		
		PropertyId referId = createReferId(refName);
		PropertyValue findValue = property(referId) ;
		if (findValue == PropertyValue.NotFound) findValue = PropertyValue.createPrimitive(null) ;
		
		findValue.append(fqn) ;
	
		tree().put(referId, findValue) ;
		touch(Touch.MODIFY) ;
		
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
	
	public <T> T transformer(Function<WriteNode, T> function){
		return function.apply(this) ;
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
		return property(createNormalId(key));
	}
	
	public boolean hasRef(String refName){
		return keys().contains(createReferId(refName)) ;
	}
	
	public boolean hasRef(String refName, Fqn fqn){
		return property(createReferId(refName)).asSet().contains(fqn.toString()) ;
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
	
	
	public String toString(){
		return this.getClass().getSimpleName() + "[fqn=" + tree().getFqn().toString() + "]";
	}

	public WriteChildren children(){
		final Iterator<TreeNode> iter = tree().getChildren().iterator();
		return new WriteChildren(session(), iter) ;
	}
	
	private void touch(Touch touch) {
		session().notifyTouch(this.fqn(), touch) ;
	}
	
	private void touch(Touch touch, Fqn target) {
		session().notifyTouch(target, touch) ;
	}

	
}
