package net.ion.craken.loaders.lucene;

import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;

import com.google.common.collect.Lists;


import net.ion.framework.util.CollectionUtil;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import junit.framework.TestCase;

public class TestExtractList extends TestCase {

	
	public void testEx() throws Exception {
		List<Key> list = ListUtil.toList(new Key("1", "Hello"), new Key("2", "Hello"), new Key("3", "Hello"), new Key("2", "Hi"), new Key("2", "Greet"));
		
		
		HashSet<Key> set = new HashSet<Key>(Lists.reverse(list));
		
		Debug.line(set) ;
	}
}


class Key {
	private String pre ;
	private String val ;
	
	public Key(String pre, String val){
		this.pre = pre ;
		this.val = val ;
	}
	
	public boolean equals(Object obj){
		Key that = (Key) obj ;
		return val.equals(that.val) ;
	}
	
	public int hashCode(){
		return val.hashCode() ;
	}
	
	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}
}
