package net.ion.craken.node;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.ion.craken.loaders.lucene.CentralCacheStore;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;

import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.atomic.AtomicMap;

public class DumpNode {

	private final DumpSession dsession;
	private final Indexer indexer;
	private final Mode mode;
	private final Fqn fqn;
	private AtomicMap<PropertyId, PropertyValue> props ;

	private enum Mode {
		INSERT, UPDATE
	}

	private DumpNode(DumpSession dsession, Indexer indexer, Mode mode, Fqn fqn) {
		this.dsession = dsession ;
		this.indexer = indexer ;
		this.mode = mode ;
		this.fqn = fqn ;
		this.props = new AtomicHashMap<PropertyId, PropertyValue>() ;
	}

	public static DumpNode insert(DumpSession dession, Indexer indexer, Fqn fqn) {
		return new DumpNode(dession, indexer, Mode.INSERT, fqn);
	}

	public static DumpNode update(DumpSession dession, Indexer indexer, Fqn fqn) {
		return new DumpNode(dession, indexer, Mode.UPDATE, fqn);
	}

	public void apply(IndexSession isession) throws IOException {
		if (this.mode == Mode.INSERT){
			isession.insertDocument(toDoc(isession)) ;
		} else {
			isession.updateDocument(toDoc(isession)) ;
		}
	}

	private WriteDocument toDoc(IndexSession isession) {
		return CentralCacheStore.toWriteDocument(isession, dsession.indexConfig(), fqn, props) ;
	}

	public DumpNode property(String key, Object value) {
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

	public DumpNode append(String key, Object... values){
		PropertyValue findValue = property(key) ;
		if (findValue == PropertyValue.NotFound) findValue = PropertyValue.createPrimitive(null) ;
		
		findValue.append(values) ;
		
		property(createNormalId(key), findValue) ;
		return this ;
	}
	
	private PropertyId createNormalId(String key){
		return PropertyId.normal(key) ; 
	}
	
	public DumpNode property(PropertyId key, PropertyValue value) {
		props.put(key, value) ;
		return this ;
	}
	
	public PropertyValue property(String key) {
		return property(createNormalId(key));
	}
	
	public PropertyValue property(PropertyId pid) {
		return ObjectUtil.coalesce(props.get(pid), PropertyValue.NotFound);
	}
	
	public Map<PropertyId, PropertyValue> toMap() {
		return Collections.unmodifiableMap(props);
	}
	

}
