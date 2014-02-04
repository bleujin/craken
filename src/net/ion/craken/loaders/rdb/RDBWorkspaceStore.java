package net.ion.craken.loaders.rdb;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.loaders.modifications.Modification;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.marshall.StreamingMarshaller;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.loaders.WorkspaceStore;
import net.ion.craken.loaders.neo.NeoWorkspaceConfig;
import net.ion.craken.loaders.neo.NodeEntry;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Action;
import net.ion.framework.db.DBController;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.manager.OracleCacheDBManager;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;
import net.ion.nsearcher.config.Central;

@CacheLoaderMetadata(configurationClass = RDBWorkspaceConfig.class)
public class RDBWorkspaceStore extends WorkspaceStore {

	private RDBWorkspaceConfig config;
	private Central central;
	private DBController dc;

	@Override
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		super.init(config, cache, m);
		this.config = (RDBWorkspaceConfig) config;
	}

	@Override
	public void start() throws CacheLoaderException {
		try {
			super.start();
			this.central = config.buildCentral();
			this.dc = config.buildDBController();
			this.dc.initSelf();
		} catch (IOException e) {
			throw new CacheLoaderException(e);
		} catch (SQLException e) {
			throw new CacheLoaderException(e);
		}
	}

	@Override
	public Central central() {
		return central;
	}

	public DBController dc() {
		return dc;
	}

	@Override
	protected void purgeInternal() throws CacheLoaderException {

	}

	@Override
	public Class<? extends CacheLoaderConfig> getConfigurationClass() {
		return RDBWorkspaceConfig.class;
	}

	@Override
	public void store(InternalCacheEntry entry) throws CacheLoaderException {
		Debug.warn("this message hasn't viewed");
	}

	@Override
	public void stop() throws CacheLoaderException {
		IOUtil.closeQuietly(central);
		dc.destroySelf();
		super.stop();
	}

	@Override
	public boolean remove(Object _key) throws CacheLoaderException {
		return false; // ignore(action in loglistener)
	}

	protected void applyModifications(List<? extends Modification> mods) throws CacheLoaderException {
		// ignore(action in loglistener)
	}

	@Override
	public void clear() throws CacheLoaderException {
		// TODO Auto-generated method stub

	}

	@Override
	public InternalCacheEntry load(Object _key) throws CacheLoaderException {
		final TreeNodeKey key = (TreeNodeKey) _key;
		try {
			if (key.action() == Action.RESET || key.action() == Action.CREATE)
				return null; // if log, return

			if (key.getType().isStructure()) {
				Rows rows = struRows(key.action(), key.fqnString());
				return RowEntry.createStru(key, rows);
			}

			Rows findRows = nodeRows(key.action(), key.idString());
			if (!findRows.next()) { // <-- alert : next
				return null;
			}

			InternalCacheEntry readObject = RowEntry.create(findRows);
			if (readObject != null && readObject.isExpired(System.currentTimeMillis())) {
				return null;
			}
			return readObject;
		} catch (SQLException ex) {
			throw new CacheLoaderException(ex);
		}
	}

	private Rows nodeRows(Action action, String fqnString) throws SQLException {
		return dc.createUserProcedure("craken@nodeBy(?)").addParam(fqnString).execQuery();
	}

	private Rows struRows(Action action, String fqnString) throws SQLException {
		return dc.createUserProcedure("craken@childrenBy(?)").addParam(fqnString).execQuery();
	}

	private Rows nodeAllRows(int limit) throws SQLException {
		return dc.createUserProcedure("craken@nodeAllBy(?)").addParam(limit).execQuery();
	}

	private Rows nodeAllFqnRows() throws SQLException {
		return dc.createUserProcedure("craken@nodeAllFqnBy()").execQuery();
	}

	@Override
	public Set<InternalCacheEntry> load(int count) throws CacheLoaderException {
		try {
			Rows rows = nodeAllRows(count);
			List<InternalCacheEntry> result = ListUtil.newList();
			while (rows.next()) {
				result.add(RowEntry.create(rows));
			}
			return new HashSet<InternalCacheEntry>(result);
		} catch (SQLException ex) {
			throw new CacheLoaderException(ex);
		}
	}

	@Override
	public Set<InternalCacheEntry> loadAll() throws CacheLoaderException {
		return load(Integer.MAX_VALUE - 1);
	}

	@Override
	public Set<Object> loadAllKeys(Set<Object> _excludesKey) throws CacheLoaderException {

		try {
			List<String> excludeKey = ListUtil.newList();
			for (Object _key : _excludesKey) {
				TreeNodeKey key = (TreeNodeKey) _key;
				excludeKey.add(key.idString());
			}
	
			Rows rows = nodeAllFqnRows() ;
			List<TreeNodeKey> result = ListUtil.newList();
			while (rows.next()) {
				final String path = rows.getString("fqn");
				if (!excludeKey.contains(path)) {
					result.add(TreeNodeKey.fromString(path));
				}
			}
			return new HashSet<Object>(result);
		} catch(SQLException ex){
			throw new CacheLoaderException(ex) ;
		}
	}

}
