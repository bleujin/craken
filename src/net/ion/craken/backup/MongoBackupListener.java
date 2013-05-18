package net.ion.craken.backup;

import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

@Listener
public class MongoBackupListener implements WorkspaceListener{

	private Mongo mongo;
	private DB db;
	private DBCollection col;
	public MongoBackupListener(String host, int port, String dbName, String colName) throws UnknownHostException{
		MongoOptions moptions = new MongoOptions();
		moptions.autoConnectRetry = true;
		moptions.connectionsPerHost = 100;
		moptions.threadsAllowedToBlockForConnectionMultiplier = 10;
		ServerAddress srvAddr = new ServerAddress(host, port);
		this.mongo = new Mongo(srvAddr, moptions);
		// setting WriteConcern to true enables fsync, however performance degradation is very big: 5-10 times!
		// It makes sense to enable it only on particular updates (1.4ms vs 12ms fsynced per 1KB update)
		mongo.setWriteConcern(WriteConcern.SAFE);
		this.db = mongo.getDB(dbName);
		this.col = db.getCollection(colName) ;
		
	}


	public static MongoBackupListener create(String host, int port, String dbName, String colName) throws UnknownHostException{
		return new MongoBackupListener(host, port, dbName, colName) ;
	}

	@CacheEntryModified
	public void entryModified(CacheEntryModifiedEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> e){
		if (e.isPre()) return ;
		
		if (e.getKey().getContents() == Type.DATA)  {
			Map outer = MapUtil.newMap() ;
			
			Map<String, Object> properties = MapUtil.newMap() ;
			Map<String, Set> refs = MapUtil.newMap() ;
			for(Entry<PropertyId, PropertyValue> entry : e.getValue().entrySet()){
				if (entry.getKey().type() == PropertyId.PType.NORMAL){
					properties.put(entry.getKey().getString(), entry.getValue().asSet());
				} else {
					refs.put(entry.getKey().getString(), entry.getValue().asSet()) ;
				}
			}
			outer.put("properties", properties) ;
			outer.put("references", refs) ;
			outer.put("_id", e.getKey().getFqn().toString()) ;
			outer.put("_lastmodified", new Date().getTime()) ;
			
			BasicDBObject dbo = new BasicDBObject(outer);
//			col.insert(dbo) ;
			col.save(dbo) ;
		}
	}

	
	
	public void registered(Workspace workspace) {
		Debug.line(this + " registered at " + workspace.wsName()) ;
	}

	public void unRegistered(Workspace workspace) {
		mongo.close() ;
		Debug.line(this + " unregistered at " + workspace.wsName()) ;
	}
}