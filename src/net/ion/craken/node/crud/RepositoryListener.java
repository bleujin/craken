package net.ion.craken.node.crud;

import static net.ion.craken.node.Repository.SYSTEM_CACHE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.ion.craken.loaders.lucene.DocEntry;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

import org.apache.lucene.analysis.kr.utils.StringUtil;
import org.apache.lucene.queryParser.ParseException;
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

	private Set<MyStatus> allStatus = new HashSet<MyStatus>();
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

		if (event.getOldMembers() == null || event.getOldMembers().size() == 0) { // newMember is actor
			executor.schedule(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					cache.put(TreeNodeKey.fromString("#/start/status/"), MapUtil.EMPTY);
					return null;
				}
			}, 1, TimeUnit.SECONDS);
		}
		this.members = event.getNewMembers();
		this.startedTime = System.currentTimeMillis();
		this.allStatus.clear();
		Debug.line(this.members, this.allStatus);
	}

	@CacheStarted
	public void startedCache(CacheStartedEvent event) throws IOException {
	}

	@CacheStopped
	public void stoppedCache(CacheStoppedEvent event) {
	}

	@CacheEntryModified
	public void entryModified(final CacheEntryModifiedEvent<TreeNodeKey, Map<PropertyId, PropertyValue>> event) throws IOException {
		final TreeNodeKey key = event.getKey();
		if (!key.getType().isSystem())
			return;
		if (event.isPre())
			return;
		if (!SYSTEM_CACHE.equals(event.getCache().getName()))
			return;

		if (key.fqnString().startsWith("/start/status")) {
			final Cache<TreeNodeKey, Map<PropertyId, PropertyValue>> cache = event.getCache();

			final MyStatus status = new MyStatus();

			status.lastModified(repository.lastSyncModified()).started(startedTime).memeberName(repository.memberName());
			repository.lastSyncModified(startedTime) ;

			final Map<PropertyId, PropertyValue> value = new AtomicHashMap<PropertyId, PropertyValue>();
			value.put(MyStatus.STATUS, PropertyValue.createPrimitive(status.toJsonString()));

			executor.submitTask(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					cache.put(TreeNodeKey.fromString("#/end/status/" + event.getCache().getName()), value);
					return null;
				}
			});
		} else if (key.fqnString().startsWith("/end/status")) {
			final MyStatus status = MyStatus.fromJson(event.getValue().get(MyStatus.STATUS).stringValue());
			allStatus.add(status);

			electMaster();
		}

	}

	private void electMaster() {
		if (members.size() == 1)
			return;
		if (members.size() != allStatus.size())
			return;

		List<MyStatus> serverStatus = new ArrayList<MyStatus>(allStatus);
		Collections.sort(serverStatus);

		final MyStatus oldNode = serverStatus.get(0);
		final MyStatus masterNode = serverStatus.get(serverStatus.size() - 1);


		if (masterNode.memberName().equals(repository.memberName())) {
			final long oldTime = oldNode.lastModified();
			Debug.line(masterNode.memberName(), oldNode.memberName(), repository.memberName());
			executor.submitTask(new Callable<Void>() {
				public Void call() throws IOException, ParseException{
					for (String wsName : repository.workspaceNames()) {
						ReadSession session = repository.login(wsName);
						List<ReadNode> modifiedNode = session.queryRequest("").gte(DocEntry.LASTMODIFIED, oldTime).find().toList();
						Debug.line(session, wsName, oldNode.lastModified(), masterNode.lastModified(), modifiedNode);
					}
					// current is master
					return null ;
				}
			}) ;
		}
	}

}

class MyStatus implements Comparable<MyStatus> {

	public final static MyStatus BLANK = new MyStatus();

	final static PropertyId STATUS = PropertyId.normal("status");
	private long started = 0L;
	private long lastModified = 0L;
	private String memberName;

	public JsonObject toJson() {
		return JsonObject.fromObject(this);
	}

	public String memberName() {
		return memberName;
	}

	public long lastModified() {
		return lastModified;
	}

	public long started() {
		return started;
	}

	public String toJsonString() {
		return toJson().toString();
	}

	public static MyStatus fromJson(String jsonString) {
		return JsonObject.fromString(jsonString).getAsObject(MyStatus.class);
	}

	public MyStatus started(long started) {
		this.started = started;
		return this;
	}

	public MyStatus lastModified(long lastModified) {
		this.lastModified = lastModified;
		return this;
	}

	public MyStatus memeberName(String memberName) {
		this.memberName = memberName;
		return this;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MyStatus))
			return false;
		MyStatus that = (MyStatus) obj;
		return StringUtil.equals(this.memberName, that.memberName);
	}

	public int hashCode() {
		return memberName.hashCode();
	}

	public String toString() {
		return toJson().toString();
	}

	@Override
	public int compareTo(MyStatus o) {
		return lastModified > o.lastModified ? 1 : (lastModified < o.lastModified ? -1 : this.memberName.compareTo(o.memberName));
	}
}
