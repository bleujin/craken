package net.ion.bleujin.infinispan;

import static org.junit.Assert.*;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.atomic.impl.AtomicHashMap;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.transaction.TransactionMode;

import junit.framework.TestCase;

public class TestCachedMap extends TestCase {

	
	public void testRun() throws Exception {
		DefaultCacheManager dcm = new DefaultCacheManager() ;
		
		dcm.defineConfiguration("test", new ConfigurationBuilder().transaction().transactionMode(TransactionMode.TRANSACTIONAL).invocationBatching().enable().build()) ;
		Cache<String, AtomicMap<PropertyId, PropertyValue>> cache = dcm.getCache("test") ;
		
		AtomicHashMap<PropertyId, PropertyValue> bleujin = new AtomicHashMap<PropertyId, PropertyValue>() ;
		bleujin.put(PropertyId.fromIdString("name"), PropertyValue.createPrimitive("bleujin")) ;
		bleujin.put(PropertyId.fromIdString("age"), PropertyValue.createPrimitive(20)) ;
		cache.put("/bleujin", bleujin) ;
		
		
		AtomicMap<PropertyId, PropertyValue> find = cache.get("/bleujin") ;
		Debug.line(find, find.get(PropertyId.fromIdString("name")));
		
		AtomicMap<String, Object> cached = AtomicMapLookup.getAtomicMap(cache, "/hero", true) ;
		cached.put("name", "hero") ;
		
		
		find = cache.get("/hero") ;
		Debug.line(find, find.get("name"));
		
		
	}
}
