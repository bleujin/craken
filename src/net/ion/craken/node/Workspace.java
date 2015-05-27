package net.ion.craken.node;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import net.ion.craken.listener.CDDMListener;
import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.loaders.EntryKey;
import net.ion.craken.mr.NodeMapReduce;
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.craken.node.crud.ReadChildrenEach;
import net.ion.craken.node.crud.ReadChildrenIterator;
import net.ion.craken.node.crud.TreeNode;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.crud.TreeStructureSupport;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.PropertyId.PType;
import net.ion.craken.tree.PropertyValue.VType;
import net.ion.framework.mte.Engine;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.apache.lucene.analysis.Analyzer;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.atomic.impl.AtomicHashMap;
import org.infinispan.context.Flag;
import org.infinispan.io.GridFilesystem;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;


public abstract class Workspace extends TreeStructureSupport implements Closeable, WorkspaceListener {

	protected Workspace(AdvancedCache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache) {
		super(cache, cache.getBatchContainer());
	}
	
	protected abstract WriteNode createNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) ;

	protected abstract WriteNode resetNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) ;

	protected abstract WriteNode writeNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) ;

	protected abstract TreeNode readNode(Fqn fqn) ;
	

	public abstract Workspace start() ;
	
	public abstract void close() ;


	
	public abstract <T> T getAttribute(String key, Class<T> clz)  ;

	public abstract String wsName() ;
	
	public abstract Workspace executorService(ExecutorService es) ;
	
	public abstract Workspace withFlag(Flag... flags) ;

	public abstract <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob)  ;

	public abstract <T> Future<T> tran(final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler ehandler) ;
	
	public abstract <T> Future<T> tran(ExecutorService exec, final WriteSession wsession, final TransactionJob<T> tjob, final TranExceptionHandler ehandler) ;

	public abstract Repository repository() ;
	
	public abstract boolean exists(Fqn f) ;

//	public abstract boolean existsData(Fqn f) ;

	public abstract Central central() ;

	public abstract void continueUnit(WriteSession wsession) throws IOException ;

	public abstract void begin() ;

	public abstract void failEnd() ;

	public abstract void end() ;

	public abstract Workspace addListener(WorkspaceListener listener) ;

	public abstract void removeListener(WorkspaceListener listener) ;

	public abstract ExecutorService executor() ;

	public abstract Engine parseEngine() ;

	public abstract <Ri, Rv> Future<Map<Ri, Rv>> mapReduce(NodeMapReduce<Ri, Rv> mapper) ;

	public abstract void remove(Fqn fqn)  ;

	@Deprecated
	public abstract Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache() ;

	public abstract GridFilesystem gfs() ;

	public abstract NodeWriter createLogWriter(WriteSession wsession, ReadSession rsession) throws IOException ;

	public abstract CDDMListener cddm()  ;

	public abstract void log(String msg) ;

	public abstract void modified(CacheEntryModifiedEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> event) ;
	
	public abstract void removed(CacheEntryRemovedEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> event) ;

	public abstract WriteSession newWriteSession(ReadSession readSession) ;

	public abstract void reindex(WriteNode wnode, Analyzer anal, boolean includeSub) ;

	protected IndexJob<Void> makeIndexJob(final WriteNode targetNode, final boolean includeSub, final IndexWriteConfig iwconfig) {
		IndexJob<Void> indexJob = new IndexJob<Void>() {
			@Override
			public Void handle(final IndexSession isession) throws Exception {

				indexNode(targetNode.toReadNode(), iwconfig, targetNode.fqn(), isession);
				if (includeSub) {
					targetNode.toReadNode().walkChildren().eachNode(new ReadChildrenEach<Void>() {
						@Override
						public Void handle(ReadChildrenIterator riter) {
							try {
								while (riter.hasNext()) {
									ReadNode next = riter.next();
									indexNode(next, iwconfig, next.fqn(), isession);
								}
							} catch (IOException ex) {
								ex.printStackTrace();
							}
							return null;
						}
					});
				}

				return null;
			}

			private void indexNode(final ReadNode wnode, final IndexWriteConfig iwconfig, final Fqn fqn, IndexSession isession) throws IOException {
				WriteDocument wdoc = isession.newDocument(fqn.toString());
				wdoc.keyword(EntryKey.PARENT, fqn.getParent().toString());
				wdoc.number(EntryKey.LASTMODIFIED, System.currentTimeMillis());

				Map<PropertyId, PropertyValue> valueMap = wnode.toMap();

				for (PropertyId pid : valueMap.keySet()) {
					PropertyValue pvalue = valueMap.get(pid);
					JsonArray jarray = pvalue.asJsonArray();
					final String propId = pid.getString();

					if (pid.type() == PType.NORMAL) {
						VType vtype = pvalue.type();
						for (JsonElement e : jarray.toArray()) {
							if (e == null)
								continue;
							FieldIndex fieldIndex = iwconfig.fieldIndex(propId);
							fieldIndex.index(wdoc, propId, vtype, e.isJsonObject() ? e.toString() : e.getAsString());
						}
					} else { // refer
						for (JsonElement e : jarray.toArray()) {
							if (e == null)
								continue;
							FieldIndex.KEYWORD.index(wdoc, '@' + propId, e.getAsString());
						}
					}
				}

				wdoc.update();
			}
		};
		return indexJob;
	}


}
