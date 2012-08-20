package net.ion.craken;

import java.io.Serializable;
import java.util.Map;

import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;

import sun.swing.StringUIClientPropertyKey;

public class SimpleDataNode implements DataNode{

	private static final long serialVersionUID = 3645106091930569034L;

	private final SimpleNodeKey skey ;
	private final Map<String, Serializable> data = MapUtil.newCaseInsensitiveMap() ;
	
	private SimpleDataNode(SimpleNodeKey skey) {
		this.skey = skey ;
	}

	public static SimpleDataNode create(String idString) {
		return new SimpleDataNode(SimpleNodeKey.create(idString));
	}

	@Override
	public NodeKey getKey() {
		return skey;
	}

	@Override
	public DataNode put(String pid, Serializable value) {
		data.put(pid, value) ;
		return this;
	}

	@Override
	public Serializable getValue(String pid) {
		return data.get(pid);
	}

}


class SimpleNodeKey implements NodeKey {

	private static final long serialVersionUID = -340773896592734843L;
	private String idString ;
	public SimpleNodeKey(String lowerId) {
		this.idString = lowerId ;
	}

	public static SimpleNodeKey create(String idString) {
		return new SimpleNodeKey(StringUtil.lowerCase(idString));
	}

	public boolean equals(Object obj){
		if (! (obj instanceof SimpleNodeKey)) return false ;
		return ((SimpleNodeKey)obj).idString.equals(this.idString) ;
	}
	
	public int hashCode(){
		return idString.hashCode() ;
	}
	
}