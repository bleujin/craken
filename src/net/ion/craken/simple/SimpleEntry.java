package net.ion.craken.simple;

import java.io.Serializable;
import java.util.Map;

import net.ion.craken.AbstractEntry;
import net.ion.craken.NodeKey;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;

public class SimpleEntry extends AbstractEntry{

	private static final long serialVersionUID = 3645106091930569034L;

	private final NodeKey skey ;
	private final Map<String, Serializable> data = MapUtil.newCaseInsensitiveMap() ;
	
	private SimpleEntry(Object id) {
		this.skey = SimpleKeyFactory.create(id) ;
	}

	@Override
	public NodeKey key() {
		return skey;
	}

	public SimpleEntry put(String pid, Serializable value) {
		data.put(pid, value) ;
		return this;
	}

	public <RET> RET rawField(String fname){
		if (fname == null || fname.length() == 0)
            return null;

		return (RET)data.get(fname) ;
	}
	
	public <RET> RET field(final String fname){
		RET value = this.<RET> rawField(fname);
		return value ;
	}
	
	public int fieldAsInt(String fname){
		return field(fname) ;
	}

	public String fieldAsString(String fname){
		return field(fname) ;
	}

	public String toString(){
		return data.toString() ;
	}

}

