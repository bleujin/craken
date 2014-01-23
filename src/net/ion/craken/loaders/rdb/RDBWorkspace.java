package net.ion.craken.loaders.rdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Map.Entry;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.loaders.CacheLoaderManager;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.loaders.WorkspaceConfig;
import net.ion.craken.loaders.neo.NeoWorkspaceStore;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.craken.tree.TreeNodeKey.Action;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.procedure.IUserCommandBatch;
import net.ion.framework.db.procedure.IUserProcedures;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.config.Central;

public class RDBWorkspace extends Workspace {

	private RDBWorkspaceConfig config;
	private RDBWorkspaceStore rstore;
	private IDBController dc;

	public RDBWorkspace(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName, RDBWorkspaceConfig config) {
		super(repository, cache, wsName, config) ;
		this.rstore = ((RDBWorkspaceStore) cache.getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class).getCacheStore());
		this.config = config;
		this.dc = rstore.dc() ;
	}

	@Override
	public Central central() {
		return rstore.central();
	}

	@Override
	public WorkspaceConfig config() {
		return config;
	}

	@Override
	protected int storeData(InputStream input) throws IOException {
		final JsonReader reader = new JsonReader(new InputStreamReader(input, "UTF-8"));
		reader.beginObject();
		String configName = reader.nextName();
		final JsonObject config = reader.nextJsonObject();
		String logName = reader.nextName();

		
		IUserProcedures upts = dc.createUserProcedures("applyTran");
		IUserCommandBatch batch = dc.createUserCommandBatch("insert into craken_tblt(seqno, fqn, action, parentFqn, props) values(?,?,?,?,?)");
		upts.add(batch) ;
		upts.add(dc.createUserProcedure("craken@storeWith()")) ;
		
		final int count = config.asInt("count");
		try {
			reader.beginArray();
			for (int seq = 0; seq < count; seq++) {
				
				JsonObject tlog = reader.nextJsonObject();
				String path = tlog.asString("path");
				Touch touch = Touch.valueOf(tlog.asString("touch"));
				Action action = Action.valueOf(tlog.asString("action"));
				String parentPath = Fqn.fromString(path).getParent().toString() ;
				// Debug.line(path, touch) ;
				switch (touch) {
				case TOUCH:
					break;
				case MODIFY:
					JsonObject val = tlog.asJsonObject("val");
					if ("/".equals(path) && val.childSize() == 0)
						continue;
					batch.addBatchParam(0, seq);
					batch.addBatchParam(1, path) ;
					batch.addBatchParam(2, touch.toString()) ;
					batch.addBatchParam(3, parentPath) ;
					batch.addBatchClob(4, val.toString()) ;

					break;
				case REMOVE:
					batch.addBatchParam(0, seq);
					batch.addBatchParam(1, path) ;
					batch.addBatchParam(2, touch.toString()) ;
					batch.addBatchParam(3, parentPath) ;
					batch.addBatchClob(4, "") ;
					break;
				case REMOVECHILDREN:
					batch.addBatchParam(0, seq);
					batch.addBatchParam(1, path) ;
					batch.addBatchParam(2, touch.toString()) ;
					batch.addBatchParam(3, parentPath) ;
					batch.addBatchClob(4, "") ;
					break;
				default:
					throw new IllegalArgumentException("Unknown modification type " + touch);
				}
			}
			
			return upts.execUpdate() ;
		} catch(SQLException ex){
			throw new IOException(ex) ;
		} finally {
			IOUtil.closeQuietly(reader) ;
		}
	}

	
	public RDBWorkspaceStore store(){
		return rstore ;
	}
	
}
