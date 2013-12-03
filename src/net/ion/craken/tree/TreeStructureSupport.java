package net.ion.craken.tree;

import java.util.Map;

import org.infinispan.AdvancedCache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.batch.AutoBatchSupport;
import org.infinispan.batch.BatchContainer;
import org.infinispan.util.concurrent.locks.LockManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

public class TreeStructureSupport extends AutoBatchSupport {
    private static final Log log = LogFactory.getLog(TreeStructureSupport.class);

	protected final AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache;

	public TreeStructureSupport(AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, BatchContainer batchContainer) {
		this.cache = cache;
		this.batchContainer = batchContainer;
	}

	public static boolean isLocked(LockManager lockManager, Fqn fqn) {
		return ((lockManager.isLocked(fqn.struKey()) && lockManager.isLocked(fqn.dataKey())));
	}

	protected final <K, V> Map<K, V> getAtomicMap(final TreeNodeKey key) {
		return AtomicMapLookup.getAtomicMap(cache, key, true);
	}

	
	public AtomicMap<PropertyId, PropertyValue> props(Fqn fqn){
		AtomicMap<PropertyId, PropertyValue> props = AtomicMapLookup.getAtomicMap(cache, fqn.dataKey(), true);
		return props ;
	}
	

	public AtomicMap<String, Fqn> strus(Fqn fqn){
		
		AtomicMap<String, Fqn> props =  AtomicMapLookup.getAtomicMap(cache, fqn.struKey(), true);
		return props ;
	} 
	
}
