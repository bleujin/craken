package net.ion.craken.loaders.lucene;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import net.ion.craken.io.GridBlob;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.io.Metadata;
import net.ion.craken.io.WritableGridBlob;
import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.loaders.EntryKey;
import net.ion.craken.loaders.WorkspaceConfig;
import net.ion.craken.mr.NodeMapReduce;
import net.ion.craken.mr.NodeMapReduceTask;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TranResult;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.node.exception.NodeNotExistsException;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeStructureSupport;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.craken.tree.TreeNodeKey.Action;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.SetUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.batch.BatchContainer;
import org.infinispan.distexec.mapreduce.Collector;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

@Listener
public class ISearchWorkspace extends Workspace {

	private ISearcherWorkspaceStore cstore;
	private ISearcherWorkspaceConfig config;

	private static final Log log = LogFactory.getLog(ISearchWorkspace.class);

	public ISearchWorkspace(Repository repository, Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName, ISearcherWorkspaceConfig config) {
		this(repository, cache.getAdvancedCache(), wsName, config);
	}

	private ISearchWorkspace(Repository repository, AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache, String wsName, ISearcherWorkspaceConfig config) {
		super(repository, cache, wsName, config);

		this.cstore = ((ISearcherWorkspaceStore) cache.getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class).getCacheStore());
		this.config = config;
	}


	protected int storeData(InputStream input) throws IOException {

		// Debug.line(IOUtil.toStringWithClose(input)) ;

		final JsonReader reader = new JsonReader(new InputStreamReader(input, "UTF-8"));
		reader.beginObject();
		String configName = reader.nextName();
		final JsonObject config = reader.nextJsonObject();
		String logName = reader.nextName();

		// Debug.line(config, wsession.tranId(), meta) ;

		int count = central().newIndexer().index(new IndexJob<Integer>() {
			public Integer handle(IndexSession isession) throws Exception {

				isession.setIgnoreBody(config.asBoolean("ignoreBody"));
				
				reader.beginArray();
				final int count = config.asInt("count");
				for (int i = 0; i < count; i++) {
					JsonObject tlog = reader.nextJsonObject();
					String path = tlog.asString("path");
					Touch touch = Touch.valueOf(tlog.asString("touch"));
					Action action = Action.valueOf(tlog.asString("action"));
					// Debug.line(path, touch) ;
					switch (touch) {
					case TOUCH:
						break;
					case MODIFY:
						JsonObject val = tlog.asJsonObject("val");
						if ("/".equals(path) && val.childSize() == 0) continue ;
						
						WriteDocument propDoc = isession.newDocument(path);
						propDoc.keyword(EntryKey.PARENT, Fqn.fromString(path).getParent().toString());
						propDoc.number(EntryKey.LASTMODIFIED, System.currentTimeMillis());
						
						JsonObject jobj = new JsonObject();
						jobj.addProperty(EntryKey.ID, path);
						jobj.addProperty(EntryKey.LASTMODIFIED, System.currentTimeMillis());
						jobj.add(EntryKey.PROPS, fromMapToJson(path, propDoc, IndexWriteConfig.read(config), val.entrySet()));
						
						propDoc.add(MyField.manual(EntryKey.VALUE, jobj.toString(), org.apache.lucene.document.Field.Store.YES, Index.NOT_ANALYZED).ignoreBody());

						if (action == Action.CREATE)
							isession.insertDocument(propDoc);
						else
							isession.updateDocument(propDoc);
						// isession.updateDocument(propDoc) ;

						break;
					case REMOVE:
						isession.deleteTerm(new Term(IKeywordField.ISKey, path));
						break;
					case REMOVECHILDREN:
						isession.deleteQuery(new WildcardQuery(new Term(EntryKey.PARENT, Fqn.fromString(path).startWith())));
						break;
					default:
						throw new IllegalArgumentException("Unknown modification type " + touch);
					}
				}

				return count;
			}

			private JsonObject fromMapToJson(String path, WriteDocument doc, IndexWriteConfig iwconfig, Set<Map.Entry<String, JsonElement>> props) {
				JsonObject jso = new JsonObject();

				for (Entry<String, JsonElement> entry : props) {
					final PropertyId propertyId = PropertyId.fromIdString(entry.getKey());

					if (propertyId.type() == PType.NORMAL) {
						String propId = propertyId.getString();
						JsonArray pvalue = entry.getValue().getAsJsonArray();
						jso.add(propId, entry.getValue().getAsJsonArray());
						for (JsonElement e : pvalue.toArray()) {
							if (e == null)
								continue;
							FieldIndex fieldIndex = iwconfig.fieldIndex(propId);
							fieldIndex.index(doc, propId, e.isJsonObject() ? e.toString() : e.getAsString());
						}
					} else if (propertyId.type() == PType.REFER) {
						final String propId = propertyId.getString();
						JsonArray pvalue = entry.getValue().getAsJsonArray();
						jso.add(propId, entry.getValue().getAsJsonArray()); // if type == refer, @
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

		reader.endArray();
		reader.endObject();
		IOUtil.closeQuietly(reader);
		return count;
	}

	
	public Central central() {
		return cstore.central();
	}

	public WorkspaceConfig config() {
		return config;
	}


}
