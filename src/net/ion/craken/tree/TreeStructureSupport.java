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

	protected final AdvancedCache<TreeNodeKey, AtomicMap<?, ?>> cache;

	@SuppressWarnings("unchecked")
	public TreeStructureSupport(AdvancedCache<?, ?> cache, BatchContainer batchContainer) {
		this.cache = (AdvancedCache<TreeNodeKey, AtomicMap<?, ?>>) cache;
		this.batchContainer = batchContainer;
	}

	public boolean exists(Fqn f) {
		startAtomic();
		try {
			return cache.get(new TreeNodeKey(f, TreeNodeKey.Type.DATA)) != null ;
//			return cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.DATA)) && cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.STRUCTURE));
		} finally {
			endAtomic();
		}
	}

	/**
	 * @param fqn
	 * @return true if created, false if this was not necessary
	 */
	protected boolean createNodeInCache(AdvancedCache<?, ?> cache, Fqn fqn) {
		startAtomic();
		try {
			TreeNodeKey dataKey = new TreeNodeKey(fqn, TreeNodeKey.Type.DATA);
			if (cache.containsKey(dataKey))
				return false;

			Fqn parent = fqn.getParent();
			if (!fqn.isRoot()) {
				if (!exists(parent))
					createNodeInCache(cache, parent);
				AtomicMap<Object, Fqn> parentStructure = getStructure(cache, parent);
				parentStructure.put(fqn.getLastElement(), fqn);
			}
			getAtomicMap(cache, dataKey);
			
			if (log.isTraceEnabled()) log.tracef("Created node %s", fqn);
			return true;
		} finally {
			endAtomic();
		}
	}

	private AtomicMap<Object, Fqn> getStructure(Fqn fqn) {
		return getStructure(new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE));
	}

	AtomicMap<Object, Fqn> getStructure(TreeNodeKey nodeKey) {
		final AtomicMap<Object, Fqn> result = getAtomicMap(nodeKey);
//		cache.keySet()
//		cache.remove(nodeKey) ;
//		Debug.line('#', result, result.keySet(), cache.get(nodeKey).keySet()) ;
		return result;
	}

	protected AtomicMap<Object, Fqn> getStructure(AdvancedCache<?, ?> cache, Fqn fqn) {
		return getAtomicMap(cache, new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE));
	}

	public static boolean isLocked(LockManager lockManager, Fqn fqn) {
		return ((lockManager.isLocked(new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE)) && lockManager.isLocked(new TreeNodeKey(fqn, TreeNodeKey.Type.DATA))));
	}

	/**
	 * Visual representation of a tree
	 * 
	 * @param cache
	 *            cache to dump
	 * @return String rep
	 */
	public static String printTree(TreeCache<?, ?> cache, boolean details) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n\n");

		// walk tree
		sb.append("+ ").append(Fqn.SEPARATOR);
		if (details)
			sb.append("  ").append(cache.getRoot().getData());
		sb.append("\n");
		addChildren(cache.getRoot(), 1, sb, details);
		return sb.toString();
	}

	private static void addChildren(TreeNode<?, ?> node, int depth, StringBuilder sb, boolean details) {
		for (TreeNode<?, ?> child : node.getChildren()) {
			for (int i = 0; i < depth; i++)
				sb.append("  "); // indentations
			sb.append("+ ");
			sb.append(child.getFqn().getLastElementAsString()).append(Fqn.SEPARATOR);
			if (details)
				sb.append("  ").append(child.getData());
			sb.append("\n");
			addChildren(child, depth + 1, sb, details);
		}
	}

	protected final <K, V> AtomicMap<K, V> getAtomicMap(TreeNodeKey key) {
		return AtomicMapLookup.getAtomicMap(cache, key, true);
	}

	protected final <K, V> AtomicMap<K, V> getAtomicMap(AdvancedCache<?, ?> cache, TreeNodeKey key) {
		return AtomicMapLookup.getAtomicMap((AdvancedCache<TreeNodeKey, AtomicMap<?, ?>>) cache, key, true);
	}

}
