package net.ion.craken.tree;

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
		return cache.get(new TreeNodeKey(f, TreeNodeKey.Type.DATA)) != null ;
//			return cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.DATA)) && cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.STRUCTURE));
	}

	/**
	 * @param fqn
	 * @return true if created, false if this was not necessary
	 */
	protected boolean mergeAncestor(Fqn fqn) {
		TreeNodeKey dataKey = new TreeNodeKey(fqn, TreeNodeKey.Type.DATA);
		if (cache.containsKey(dataKey))
			return false;

		if (!fqn.isRoot()) {
			Fqn parent = fqn.getParent();
			if (!exists(parent))
				mergeAncestor(parent);
			AtomicMap<Object, Fqn> parentStructure = getStructure(parent);
			parentStructure.put(fqn.getLastElement(), fqn);
		}
		getAtomicMap(dataKey);
		
		return true;
	}

	/**
	 * @param fqn
	 * @return true if created, false if this was not necessary
	 */
	private boolean mergeSelf(Fqn fqn) {
		TreeNodeKey dataKey = new TreeNodeKey(fqn, TreeNodeKey.Type.DATA);
		if (cache.containsKey(dataKey))
			return false;

		Fqn parent = fqn.getParent();
		AtomicMap<Object, Fqn> parentStructure = getStructure(parent);
		parentStructure.put(fqn.getLastElement(), fqn);
		
		getAtomicMap(dataKey);
		
		if (log.isTraceEnabled()) log.tracef("Created node %s", fqn);
		return true;
	}


	
	AtomicMap<Object, Fqn> getStructure(TreeNodeKey nodeKey) {
		final AtomicMap<Object, Fqn> result = getAtomicMap(nodeKey);
//		cache.keySet()
//		cache.remove(nodeKey) ;
//		Debug.line('#', result, result.keySet(), cache.get(nodeKey).keySet()) ;
		return result;
	}

	protected AtomicMap<Object, Fqn> getStructure(Fqn fqn) {
		return getAtomicMap(new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE));
	}

	public static boolean isLocked(LockManager lockManager, Fqn fqn) {
		return ((lockManager.isLocked(new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE)) && lockManager.isLocked(new TreeNodeKey(fqn, TreeNodeKey.Type.DATA))));
	}

	protected final <K, V> AtomicMap<K, V> getAtomicMap(TreeNodeKey key) {
		return AtomicMapLookup.getAtomicMap(cache, key, true);
	}

}
