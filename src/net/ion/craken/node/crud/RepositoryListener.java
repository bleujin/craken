package net.ion.craken.node.crud;

import static net.ion.craken.node.Repository.SYSTEM_CACHE;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.TransactionLog;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ServerStatus.ElectRecent;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

import org.apache.lucene.analysis.kr.utils.StringUtil;
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
	public void entryModified(final CacheEntryModifiedEvent<TreeNodeKey, Map<PropertyId, PropertyValue>> event) throws IOException, ParseException {
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
			repository.lastSyncModified(startedTime) ; // set 
		}

	}

	private void electMaster() {
		if (members.size() == 1)
			return;
		if (members.size() != allStatus.size())
			return;

		final Map<String, Long> recentWsNames = ElectRecent.elect(allStatus, repository.memberName(), repository.workspaceNames());
		
		executor.submitTask(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				
				try {
				
					Debug.line(recentWsNames) ;
					
					for (Entry<String, Long> entry : recentWsNames.entrySet()) {  // per worksapce
						String wsName = entry.getKey() ;
						final long minTranTime = entry.getValue() ;
						final ReadSession readSession = repository.login(wsName);
						
						final List<ReadNode> recentTrans = readSession.logManager().recentTran(minTranTime) ;
						Debug.line(minTranTime, recentTrans) ;

//						for (final ReadNode tran : recentTrans ) { // per transaction
//							readSession.tranSync(new TransactionJob<Void>() {
//								@Override
//								public Void handle(WriteSession wsession) throws Exception {
//									WriteNode tranNode = wsession.pathBy(tran.fqn()) ;
////									tranNode.touch();
//									for (WriteNode log : tranNode.children()) {
//										log.touch() ;
//									}
//									
//									return null;
//								}
//							}) ;
//						}
					}
				
				
				} catch(Throwable ex){
					ex.printStackTrace();
				}
				return null;
				
				
			}
			
		}) ;
		
	}

}

class ServerStatus  {

	public final static ServerStatus BLANK = new ServerStatus();
	final transient static PropertyId STATUS = PropertyId.normal("status");
	private long started = 0L;
	private Map<String, Long> lastTran ;
	private String memberName;

	public JsonObject toJson() {
		return JsonObject.fromObject(this);
	}

	public String memberName() {
		return memberName;
	}

	public Map<String, Long> lastModified() {
		return lastTran;
	}

	public long started() {
		return started;
	}

	public String toJsonString() {
		return toJson().toString();
	}

	public static ServerStatus fromJson(String jsonString) {
		return JsonObject.fromString(jsonString).getAsObject(ServerStatus.class);
	}

	public ServerStatus started(long started) {
		this.started = started;
		return this;
	}

	public ServerStatus lastTran(Map<String, Long> lastTran) {
		this.lastTran = lastTran;
		return this;
	}

	public ServerStatus memeberName(String memberName) {
		this.memberName = memberName;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ServerStatus))
			return false;
		ServerStatus that = (ServerStatus) obj;
		return StringUtil.equals(this.memberName, that.memberName);
	}

	public int hashCode() {
		return memberName.hashCode();
	}

	public String toString() {
		return toJson().toString();
	}

	static class ElectRecent{
		public static Map<String, Long> elect(Set<ServerStatus> statusSet, String selfName, Set<String> wsNames){
			
			
			Debug.line(statusSet, selfName, wsNames) ;
			
			Map<String, Long> result = MapUtil.newMap() ;
			for (String wsName : wsNames) {
				long lastTime = 0L ;
				long minTime = Long.MAX_VALUE ;
				String recentMemberName = "" ;
				for (ServerStatus ss : statusSet) {
					Map<String, Long> lastTrans = ss.lastModified();
					Long lastTranTime = lastTrans.get(wsName);
					if (lastTranTime == null) continue ;
					if (lastTime < lastTranTime || (lastTime == lastTranTime && selfName.compareTo(ss.memberName()) > 0)){
						lastTime = lastTranTime ;
						recentMemberName = ss.memberName() ;
					}
					if (minTime > lastTranTime) minTime = lastTranTime ;
				}
				if (selfName.equals(recentMemberName)) {
					result.put(wsName, minTime) ;
				}
			}
			
			return result ;
		}
	}
	
}
