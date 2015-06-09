package net.ion.craken.node.dist;

import java.util.Map;
import java.util.Set;

import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;

public class ServerStatus {

	public final static ServerStatus BLANK = new ServerStatus();
	public final transient static PropertyId STATUS = PropertyId.normal("status");
	private long started = 0L;
	private Map<String, Long> lastTran;
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

	static class ElectRecent {
		public static Map<String, Long> elect(Set<ServerStatus> statusSet, String selfName, Set<String> wsNames) {

			Map<String, Long> result = MapUtil.newMap();
			for (String wsName : wsNames) {
				long lastTime = 0L;
				long minTime = Long.MAX_VALUE;
				String recentMemberName = "";
				for (ServerStatus ss : statusSet) {
					Map<String, Long> lastTrans = ss.lastModified();
					Long lastTranTime = lastTrans.get(wsName);
					if (lastTranTime == null)
						continue;
					if (lastTime < lastTranTime || (lastTime == lastTranTime && selfName.compareTo(ss.memberName()) > 0)) {
						lastTime = lastTranTime;
						recentMemberName = ss.memberName();
					}
					if (minTime > lastTranTime)
						minTime = lastTranTime;
				}
				if (selfName.equals(recentMemberName)) {
					result.put(wsName, minTime);
				}
			}

			return result;
		}
	}

	public static Map<String, Long> elect(Set<ServerStatus> statusSet, String selfName, Set<String> wsNames) {
		return ElectRecent.elect(statusSet, selfName, wsNames);
	}

}