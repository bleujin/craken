package net.ion.craken.tree;

import java.util.Map;

import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

import org.apache.ecs.xhtml.map;
import org.infinispan.AdvancedCache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.atomic.AtomicMapLookup;
import org.infinispan.batch.AutoBatchSupport;
import org.infinispan.batch.BatchContainer;
import org.infinispan.util.concurrent.locks.LockManager;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import scala.collection.mutable.HashMap;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

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
		final boolean result = cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.DATA)) && cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.STRUCTURE));
		return result;
//			return cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.DATA)) && cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.STRUCTURE));
	}

	/**
	 * @param fqn
	 * @return true if created, false if this was not necessary
	 */
	protected boolean mergeAncestor(Fqn fqn) {
		return mergeAncestor(IndexWriteConfig.Default, fqn);
	}

	protected boolean mergeAncestor(IndexWriteConfig iwconfig, Fqn fqn) {
		TreeNodeKey dataKey = new TreeNodeKey(fqn, TreeNodeKey.Type.DATA);
		if (cache.containsKey(dataKey))
			return false;

		if (!fqn.isRoot()) {
			Fqn parent = fqn.getParent();
			if (!exists(parent))
				mergeAncestor(iwconfig, parent);
			Map<Object, Fqn> parentStructure = getStructure(parent);
//			Debug.debug(parent, fqn.getLastElement(), fqn) ;
			
			if (! fqn.getLastElement().toString().startsWith("__")) 
				parentStructure.put(fqn.getLastElement(), fqn); // /__... 은 /의 children이 아닌걸로 .. 
		}
		getAtomicMap(dataKey);
		
		return true;
	}

	Map<Object, Fqn> getStructure(TreeNodeKey nodeKey) {
		final Map<Object, Fqn> result = getAtomicMap(nodeKey);
		
		return result;
	}

	protected Map<Object, Fqn> getStructure(Fqn fqn) {
		return getAtomicMap(new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE));
	}

	public static boolean isLocked(LockManager lockManager, Fqn fqn) {
		return ((lockManager.isLocked(new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE)) && lockManager.isLocked(new TreeNodeKey(fqn, TreeNodeKey.Type.DATA))));
	}

	protected final <K, V> Map<K, V> getAtomicMap(final TreeNodeKey key) {
		final AtomicMap<K, V> cached = AtomicMapLookup.getAtomicMap(cache, key, true);

		
		return cached ;

	}

}
