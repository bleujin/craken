package net.ion.craken.loaders.rdb;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.loaders.CacheLoaderManager;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.loaders.WorkspaceConfig;
import net.ion.craken.loaders.neo.NeoWorkspaceStore;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.craken.tree.TreeNodeKey.Action;
import net.ion.framework.db.DBController;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.procedure.IUserCommandBatch;
import net.ion.framework.db.procedure.IUserProcedures;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

public class RDBWorkspace extends Workspace {

	private RDBWorkspaceConfig config;
	private RDBWorkspaceStore rstore;
	private DBController dc;

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

		int count = central().newIndexer().index(new IndexJob<Integer>() {
			public Integer handle(IndexSession isession) throws Exception {

				isession.setIgnoreBody(config.asBoolean("ignoreBody"));
				IUserProcedures upts = dc.createUserProcedures("applyTran");
				IUserCommandBatch batch = dc.createUserCommandBatch("insert into craken_tblt(seqno, fqn, action, parentFqn, props) values(?,?,?,?,?)");
				upts.add(batch) ;
				upts.add(dc.createUserProcedure("craken@storeWith()")) ;
				
				
				reader.beginArray();
				final int count = config.asInt("count");
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
						if ("/".equals(path) && val.childSize() == 0) continue ;

						batch.addBatchParam(0, seq);
						batch.addBatchParam(1, path) ;
						batch.addBatchParam(2, touch.toString()) ;
						batch.addBatchParam(3, parentPath) ;
						batch.addBatchClob(4, val.toString()) ;

						{
							WriteDocument propDoc = isession.newDocument(path);
							propDoc.keyword(EntryKey.PARENT, Fqn.fromString(path).getParent().toString());
							propDoc.number(EntryKey.LASTMODIFIED, System.currentTimeMillis());
							
							JsonObject jobj = new JsonObject();
							jobj.addProperty(EntryKey.ID, path);
							jobj.addProperty(EntryKey.LASTMODIFIED, System.currentTimeMillis());
							
							fromMapToJson(path, propDoc, IndexWriteConfig.read(config), val.entrySet()) ;
//							propDoc.add(MyField.manual(EntryKey.VALUE, jobj.toString(), org.apache.lucene.document.Field.Store.YES, Index.NOT_ANALYZED).ignoreBody());
	
							if (action == Action.CREATE)
								isession.insertDocument(propDoc);
							else
								isession.updateDocument(propDoc);
							// isession.updateDocument(propDoc) ;
						}
						
						break;
					case REMOVE:
						batch.addBatchParam(0, seq);
						batch.addBatchParam(1, path) ;
						batch.addBatchParam(2, touch.toString()) ;
						batch.addBatchParam(3, parentPath) ;
						batch.addBatchClob(4, "") ;
						
						isession.deleteTerm(new Term(IKeywordField.ISKey, path));
						break;
					case REMOVECHILDREN:
						batch.addBatchParam(0, seq);
						batch.addBatchParam(1, path) ;
						batch.addBatchParam(2, touch.toString()) ;
						batch.addBatchParam(3, parentPath) ;
						batch.addBatchClob(4, "") ;
						
						isession.deleteQuery(new WildcardQuery(new Term(EntryKey.PARENT, Fqn.fromString(path).startWith())));
						break;
					default:
						throw new IllegalArgumentException("Unknown modification type " + touch);
					}
				}

				upts.execUpdate() ;
				return count;
			}

			
			private JsonObject fromMapToJson(String path, WriteDocument doc, IndexWriteConfig iwconfig, Set<Map.Entry<String, JsonElement>> props) {
				JsonObject jso = new JsonObject();

				for (Entry<String, JsonElement> entry : props) {
					final PropertyId propertyId = PropertyId.fromIdString(entry.getKey());

					if (propertyId.type() == PType.NORMAL) {
						String propId = propertyId.getString();
						JsonArray pvalue = entry.getValue().getAsJsonObject().asJsonArray("vals");
						jso.add(propertyId.idString(), entry.getValue());
						for (JsonElement e : pvalue.toArray()) {
							if (e == null)
								continue;
							FieldIndex fieldIndex = iwconfig.fieldIndex(propId);
							fieldIndex.index(doc, propId, e.isJsonObject() ? e.toString() : e.getAsString());
						}
					} else if (propertyId.type() == PType.REFER) {
						final String propId = propertyId.getString();
						JsonArray pvalue = entry.getValue().getAsJsonObject().asJsonArray("vals");
						jso.add(propertyId.idString(), entry.getValue()); // if type == refer, @
						for (JsonElement e : pvalue.toArray()) {
							if (e == null)
								continue;
							FieldIndex.KEYWORD.index(doc, '@' + propId, e.getAsString());
						}
					}
				}
				return jso;
			}
		});
		
		IOUtil.close(reader) ;
		return count ; 
	}

	
	public RDBWorkspaceStore store(){
		return rstore ;
	}
	
	public void close() {
		super.close(); 
		IOUtil.closeQuietly(dc);
		dc.destroySelf();
	}
	
}
