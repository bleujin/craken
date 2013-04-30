package net.ion.craken.tree;

import java.util.logging.Level;

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
		return exists(cache, f);
	}

	protected boolean exists(AdvancedCache<?, ?> cache, Fqn f) {
		startAtomic();
		try {
			return cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.DATA)) && cache.containsKey(new TreeNodeKey(f, TreeNodeKey.Type.STRUCTURE));
		} finally {
			endAtomic();
		}
	}

	/**
	 * @param fqn
	 * @return true if created, false if this was not necessary
	 */
	boolean createNodeInCache(Fqn fqn) {
		startAtomic();
		try {
			TreeNodeKey dataKey = new TreeNodeKey(fqn, TreeNodeKey.Type.DATA);
			TreeNodeKey structureKey = new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE);
			if (cache.containsKey(dataKey) && cache.containsKey(structureKey))
				return false;
			Fqn parent = fqn.getParent();
			if (!fqn.isRoot()) {
				if (!exists(parent))
					createNodeInCache(parent);
				AtomicMap<Object, Fqn> parentStructure = getStructure(parent);
				parentStructure.put(fqn.getLastElement(), fqn);
			}
			getAtomicMap(structureKey);
			getAtomicMap(dataKey);

			if (log.isTraceEnabled()) log.tracef("Created node %s", fqn);
			return true;
		} finally {
			endAtomic();
		}
	}

	protected boolean createNodeInCache(AdvancedCache<?, ?> cache, Fqn fqn) {
		startAtomic();
		try {
			TreeNodeKey dataKey = new TreeNodeKey(fqn, TreeNodeKey.Type.DATA);
			TreeNodeKey structureKey = new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE);

			if (cache.containsKey(dataKey) && cache.containsKey(structureKey))
				return false;

			Fqn parent = fqn.getParent();
			if (!fqn.isRoot()) {
				if (!exists(cache, parent))
					createNodeInCache(cache, parent);
				AtomicMap<Object, Fqn> parentStructure = getStructure(cache, parent);

				parentStructure.put(fqn.getLastElement(), fqn);
			}
			getAtomicMap(cache, structureKey);
			getAtomicMap(cache, dataKey);
			
			if (log.isTraceEnabled()) log.tracef("Created node %s", fqn);
			return true;
		} finally {
			endAtomic();
		}
	}

	AtomicMap<Object, Fqn> getStructure(Fqn fqn) {
		return getAtomicMap(new TreeNodeKey(fqn, TreeNodeKey.Type.STRUCTURE));
	}

	private AtomicMap<Object, Fqn> getStructure(AdvancedCache<?, ?> cache, Fqn fqn) {
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
		final AtomicMap result = AtomicMapLookup.getAtomicMap(cache, key);
		return result;
	}

	protected final <K, V> AtomicMap<K, V> getAtomicMap(AdvancedCache<?, ?> cache, TreeNodeKey key) {
		final AtomicMap result = AtomicMapLookup.getAtomicMap((AdvancedCache<TreeNodeKey, AtomicMap<?, ?>>) cache, key);
		return result;
	}

}
