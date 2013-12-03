package net.ion.craken.node.crud;

import static net.ion.craken.node.Repository.SYSTEM_CACHE;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ion.craken.node.AbstractWriteSession;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.dist.ServerStatus;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.logging.LogBroker;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.MapUtil;

import org.apache.lucene.queryparser.classic.ParseException;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;


@Listener(sync = false)
public class RepositoryListener {

	private Set<ServerStatus> allStatus = new HashSet<ServerStatus>();
	private IExecutor executor;
	private long startedTime;
	private List<Address> members;
	private RepositoryImpl repository;
	private Logger logger = LogBroker.getLogger(RepositoryListener.class) ;

	public RepositoryListener(RepositoryImpl repository) {
		this.repository = repository;
		this.executor = repository.executor();
	}

	@ViewChanged
	public void viewChanged(ViewChangedEvent event) {
		final Cache<TreeNodeKey, Map<PropertyId, PropertyValue>> cache = event.getCacheManager().getCache(SYSTEM_CACHE);

		if ( (event.getOldMembers() == null || event.getOldMembers().size() == 0) && event.getNewMembers().size() != 1) { // newMember is actor
			executor.schedule(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					cache.put(TreeNodeKey.fromString("#/start/status/"), MapUtil.EMPTY);
					return null;
				}
			}, 2, TimeUnit.SECONDS);
		}
		this.members = event.getNewMembers();
		this.startedTime = System.currentTimeMillis();
		this.allStatus.clear();
	}

	@CacheStarted
	public void startedCache(CacheStartedEvent event) throws IOException {
	}

	@CacheStopped
	public void stoppedCache(CacheStoppedEvent event) {
	}

	@CacheEntryModified
	public void entryModified(final CacheEntryModifiedEvent<TreeNodeKey, Map<PropertyId, PropertyValue>> event) throws IOException, ParseException, ExecutionException {
		final TreeNodeKey key = event.getKey();
		if (!key.getType().isSystem())
			return;
		if (event.isPre())
			return;
		if (!SYSTEM_CACHE.equals(event.getCache().getName()))
			return;

		if (key.fqnString().startsWith("/start/status")) {
			final Cache<TreeNodeKey, Map<PropertyId, PropertyValue>> cache = event.getCache();

			final ServerStatus status = new ServerStatus();

			status.lastTran(repository.lastSyncModified()).started(startedTime).memeberName(repository.memberName());

			final Map<PropertyId, PropertyValue> value = new AtomicHashMap<PropertyId, PropertyValue>();
			value.put(ServerStatus.STATUS, PropertyValue.createPrimitive(status.toJsonString()));
			
			executor.submitTask(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					cache.put(TreeNodeKey.fromString("#/end/status/" + event.getCache().getName()), value);
					return null;
				}
			});
		} else if (key.fqnString().startsWith("/end/status")) {
			final ServerStatus receivedStatus = ServerStatus.fromJson(event.getValue().get(ServerStatus.STATUS).stringValue());
			allStatus.add(receivedStatus);

			electMaster();
		}

	}

	private void electMaster() {
		if (members.size() == 1)
			return;
		if (members.size() != allStatus.size())
			return;

		final Map<String, Long> recentWsNames = ServerStatus.elect(allStatus, repository.memberName(), repository.workspaceNames());
		
		executor.submitTask(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				
				try {
				
					for (Entry<String, Long> entry : recentWsNames.entrySet()) {  // per worksapce
						String wsName = entry.getKey() ;
						final long minTranTime = entry.getValue() ;
						final ReadSession readSession = repository.login(wsName);
						
						final List<ReadNode> recentTrans = readSession.logManager().recentTran(minTranTime) ;

						for (final ReadNode tran : recentTrans ) { // per transaction
							readSession.tranSync(new TransactionJob<Void>() {
								@Override
								public Void handle(WriteSession wsession) throws Exception {
									((AbstractWriteSession)wsession).restoreOverwrite() ;
									WriteNode findNode = wsession.pathBy(tran.fqn()) ;
									logger.log(Level.INFO, findNode.fqn() + " apply") ;
									findNode.property("address", repository.dm().getAddress().toString()) ;
									return null;
								}
							}) ;
						}
						
//						readSession.workspace().getCache().cache().clear() ;
					}
					
					
				
				
				} catch(Throwable ex){
					ex.printStackTrace();
				}
				return null;
				
				
			}
			
		}) ;
		
	}

}

