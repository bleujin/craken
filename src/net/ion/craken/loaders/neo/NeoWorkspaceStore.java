package net.ion.craken.loaders.neo;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.loaders.WorkspaceStore;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.crud.TreeNodeKey.Action;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.config.Central;

import org.infinispan.Cache;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.loaders.modifications.Modification;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.StreamingMarshaller;
import org.infinispan.remoting.transport.Address;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

@CacheLoaderMetadata(configurationClass = NeoWorkspaceConfig.class)
public class NeoWorkspaceStore extends WorkspaceStore {

	private NeoWorkspaceConfig config;
	private Central central;
	private GraphDatabaseService graphDB;

	@Override
	public Class<? extends CacheLoaderConfig> getConfigurationClass() {
		return NeoWorkspaceConfig.class;
	}

	@Override
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		super.init(config, cache, m);
		this.config = (NeoWorkspaceConfig) config;
		EmbeddedCacheManager dm = cache.getCacheManager();
	}

	@Override
	public void start() throws CacheLoaderException {
		try {
			// open the data file
			super.start();
			this.central = config.buildCentral();
			this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(config.neoLocation());

		} catch (IOException e) {
			throw new CacheLoaderException(e);
		}
	}

	@Override
	public Central central() {
		return central;
	}

	GraphDatabaseService graphDB() {
		return graphDB;
	}

	protected void applyModifications(List<? extends Modification> mods) throws CacheLoaderException {
		// ignore(action in loglistener)
	}

	@Override
	public boolean remove(Object _key) throws CacheLoaderException {
		return false ; // ignore(action in loglistener)
	}

	@Override
	public InternalCacheEntry load(Object _key) throws CacheLoaderException {
		final TreeNodeKey key = (TreeNodeKey) _key;
		if (key.action() == Action.RESET || key.action() == Action.CREATE)
			return null; // if log, return

		if (key.getType().isStructure()) {
			Node node = mergeNode(key.action(), key.fqnString());
			return NodeEntry.createStru(key, node.getRelationships(Direction.OUTGOING));
		}

		Node findNode = findNode(key.action(), key.idString());
		if (findNode == null) {
			return null;
		}

		InternalCacheEntry readObject = NodeEntry.create(findNode);
		if (readObject != null && readObject.isExpired(System.currentTimeMillis())) {
			return null;
		}
		return readObject;
	}

	@Override
	public Set<InternalCacheEntry> load(int count) throws CacheLoaderException {
		Iterator<Node> nodes = graphDB.getAllNodes().iterator();
		List<InternalCacheEntry> result = ListUtil.newList();
		int index = 0;
		while (nodes.hasNext() && (index++ < count)) {
			final Node node = nodes.next();
			if (!node.hasProperty(EntryKey.ID))
				continue;

			result.add(NodeEntry.create(node));
		}
		// TODO Auto-generated method stub
		return new HashSet<InternalCacheEntry>(result);
	}

	@Override
	public Set<InternalCacheEntry> loadAll() throws CacheLoaderException {
		Iterator<Node> nodes = graphDB.getAllNodes().iterator();
		List<InternalCacheEntry> result = ListUtil.newList();
		while (nodes.hasNext()) {
			final Node node = nodes.next();
			if (!node.hasProperty(EntryKey.ID))
				continue;
			result.add(NodeEntry.create(node));
		}
		return new HashSet<InternalCacheEntry>(result);
	}

	@Override
	public Set<Object> loadAllKeys(Set<Object> _excludesKey) throws CacheLoaderException {

		List<String> excludeKey = ListUtil.newList();
		for (Object _key : _excludesKey) {
			TreeNodeKey key = (TreeNodeKey) _key;
			excludeKey.add(key.idString());
		}

		Iterator<Node> nodes = graphDB.getAllNodes().iterator();
		List<TreeNodeKey> result = ListUtil.newList();
		while (nodes.hasNext()) {
			final Node node = nodes.next();
			final Object path = node.getProperty(EntryKey.ID);
			if (!excludeKey.contains(path)) {
				result.add(TreeNodeKey.fromString(path.toString()));
			}
		}
		return new HashSet<Object>(result);
	}


	@Override
	public void clear() throws CacheLoaderException {
		 Transaction tx = graphDB.beginTx();
		 try {
			 Iterator<Node> iter = graphDB.getAllNodes().iterator();
			 List<Long> ids = ListUtil.newList() ;
			 while(iter.hasNext()){
			 	final Node next = iter.next();
			 	if (next.getId() == 0L) continue ;
			 	
			 	Iterator<Relationship> rels = next.getRelationships().iterator();
			 	while(rels.hasNext()){
			 		rels.next().delete() ;
			 	}
		
				ids.add(next.getId()) ;
			 }
			 for(Long id : ids){
				 graphDB.getNodeById(id).delete() ;
			 }
			 tx.success() ;
		 } catch(Exception ex){
			 tx.failure() ;
			 throw new CacheLoaderException(ex) ;
		 } finally {
			 tx.finish() ;
		 }
	}

	
	
	@Override
	public void store(InternalCacheEntry entry) throws CacheLoaderException {
		Debug.warn("this message hasn't viewed") ;
	}

	@Override
	public void stop() throws CacheLoaderException {
		IOUtil.closeQuietly(central);
		super.stop();
	}

	@Override
	protected void purgeInternal() throws CacheLoaderException {

	}

	private Node findNode(Action action, String rels) {
		return NeoWorkspace.findNode(graphDB, action, StringUtil.split(rels, "/"));
	}

	private Node mergeNode(Action action, String rels) throws CacheLoaderException {
		return NeoWorkspace.mergeNode(graphDB, action, StringUtil.split(rels, "/"));
	}

}
