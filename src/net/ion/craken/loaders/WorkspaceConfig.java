package net.ion.craken.loaders;

import java.io.IOException;

import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.loaders.AbstractCacheStoreConfig;

public abstract class WorkspaceConfig extends AbstractCacheStoreConfig {

	public final static String Location = "location";
	public final static String MaxEntries = "maxEntries";
	public final static String ChunkSize = "chunkSize";

	private static final long serialVersionUID = -449909701034789210L;

	public abstract String location();

	public abstract int maxNodeEntry();

	public abstract int lockTimeoutMs();

	public abstract Workspace createWorkspace(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName) throws IOException ;
}
