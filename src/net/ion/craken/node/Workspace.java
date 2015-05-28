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


public interface Workspace extends Closeable, WorkspaceListener {

	public abstract WriteNode createNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) ;

	public abstract WriteNode resetNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) ;

	public abstract WriteNode writeNode(WriteSession wsession, Set<Fqn> ancestorsFqn, Fqn fqn) ;

	public abstract TreeNode readNode(Fqn fqn) ;
	
	public AtomicMap<PropertyId, PropertyValue> props(Fqn fqn) ;
	
	public AtomicMap<String, Fqn> strus(Fqn fqn) ;

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


}
