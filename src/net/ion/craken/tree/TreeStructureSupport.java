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

	public boolean exists(Fqn f) {
		if (Fqn.ROOT.equals(f)) {
			return true ;
		}
		final boolean result = cache.containsKey(f.dataKey()) && cache.containsKey(f.struKey());
		return result;
//			return cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.DATA)) && cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.STRUCTURE));
	}

	protected boolean mergeAncestor(Fqn fqn) {
		if (cache.containsKey(fqn.dataKey()))
			return false;

		if (!fqn.isRoot()) {
			Fqn parent = fqn.getParent();
			if (!exists(parent))
				mergeAncestor(parent);
			
			Map<Object, Fqn> parentStructure = strus(parent);
			parentStructure.put(fqn.getLastElement(), fqn);
		}
		
		
		getAtomicMap(fqn.dataKey());
		getAtomicMap(fqn.struKey());
		
		return true;
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
	

	public AtomicMap<Object, Fqn> strus(Fqn fqn){
		AtomicMap<Object, Fqn> props =  AtomicMapLookup.getAtomicMap(cache, fqn.struKey(), true);
		return props ;
	} 
	
}
