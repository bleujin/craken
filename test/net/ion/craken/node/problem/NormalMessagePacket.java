package net.ion.craken.node.problem;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Map;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.JsonUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;

public class NormalMessagePacket implements Serializable {

	private static final long serialVersionUID = 1390688854536525148L;
	public static final NormalMessagePacket EMPTY = NormalMessagePacket.load("{}");
	public static final NormalMessagePacket PING = NormalMessagePacket.load("{head:{command:'ping'},body:{}}");

	private final transient JsonObject root;
	private final String currentPath;
	private final transient JsonObject current;

	private Map<String, NormalMessagePacket> childMap;

	private NormalMessagePacket(JsonObject root) {
		this.root = root;
		this.currentPath = "";
		this.current = root;
		this.childMap = MapUtil.newCaseInsensitiveMap();
		childMap.put("", this);
	}

	private NormalMessagePacket(JsonObject root, String currentPath, JsonObject current, Map<String, NormalMessagePacket> childMap) {
		this.root = root;
		this.currentPath = currentPath;
		this.current = current;
		this.childMap = childMap;
	}

	public final static NormalMessagePacket load(String message) {
		return new NormalMessagePacket(JsonParser.fromString(message).getAsJsonObject());
	}

	public static NormalMessagePacket create() {
		return new NormalMessagePacket(new JsonObject());
	}

	public static NormalMessagePacket load(JsonObject jsonObject) {
		return new NormalMessagePacket(jsonObject);
	}

	public NormalMessagePacket inner(String _inname) {
		String inname = _inname.toLowerCase();
		if (!current.has(inname)) {
			current.add(inname, new JsonObject());
		}

		String newPath = (isRoot() ? "" : currentPath + ".") + inname;
		if (!childMap.containsKey(newPath)) {
			JsonObject newCurrent = current.asJsonObject(inname);
			childMap.put(newPath, new NormalMessagePacket(root, newPath, newCurrent, this.childMap));
		}

		return childMap.get(newPath);
	}

	public Object get(String _path) {
		return get(this.current, _path);
	}

	public Object get(JsonObject stdObj, String path) {
		return JsonUtil.findSimpleObject(stdObj, path);
	}

	public String getString(String path) {
		return ObjectUtil.toString(get(this.current, path));
	}

	public int getInt(String path, int dftvalue) {
		Object object = get(path);
		return Integer.parseInt(ObjectUtil.toString(object, dftvalue + ""));
	}

	public String getString(String path, String dftValue) {
		String result = getString(path);
		if (StringUtil.isBlank(result))
			return dftValue;
		return result;
	}

	public NormalMessagePacket toParent() {
		String parentPath = "";
		if (StringUtil.contains(currentPath, ".")) {
			parentPath = StringUtil.substringBeforeLast(currentPath, ".");
		}

		return childMap.get(parentPath);
	}

	public NormalMessagePacket toRoot() {
		return childMap.get("");
	}

	public String getFullString() {
		return root.toString();
	}

	public boolean has(String path) {
		return !StringUtil.isBlank(getString(path));
	}

	public NormalMessagePacket put(String key, Object value) {
		current.put(key, value);

		return this;
	}

	public NormalMessagePacket array(String key, Object[] values) {
		return put(key, values);
	}

	public Map<String, ? extends Object> toMap() {
		return current.toMap();
	}

	private boolean isRoot() {
		return StringUtil.isBlank(this.currentPath);
	}

	public String toString() {
		return getFullString();
	}

	public boolean isPing() {
		return "ping".equals(getString("head.command")) || "keepalive".equals(getString("head.command")) || "app.keepalive".equals(getString("head.command"));
	}
	
	private Object writeReplace() throws ObjectStreamException {
		return new NormalMessagePacketProxy(root.toString()) ;
	}
	
}


class NormalMessagePacketProxy implements Serializable {

	private static final long serialVersionUID = 8157687420750295896L;
	private String jsonExpression;
	public NormalMessagePacketProxy(String jsonExpression) {
		this.jsonExpression = jsonExpression ;
	}

	private Object readResolve() throws java.io.ObjectStreamException {
        return NormalMessagePacket.load(this.jsonExpression);
    }
}
