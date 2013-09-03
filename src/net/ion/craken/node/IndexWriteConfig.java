package net.ion.craken.node;

import java.util.Map;

import net.ion.craken.tree.PropertyId;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;

public class IndexWriteConfig {

	public enum FieldIndex {
		UNKNOWN, IGNORE, KEYWORD, NUMBER, DATETIME, TEXT
	}
	
	private Map<String, FieldIndex> fieldIndexes = MapUtil.newMap() ;
	private boolean ignoreBody;  

	
	public final static IndexWriteConfig Default = new IndexWriteConfig() ;
	
	
	public IndexWriteConfig ignore(String... fields){
		for (String field : fields) {
			fieldIndexes.put(field, FieldIndex.IGNORE) ;
		}
		
		return this ;
	}

	public IndexWriteConfig keyword(String... fields) {
		for (String field : fields) {
			fieldIndexes.put(field, FieldIndex.KEYWORD) ;
		}
		return this ;
	}

	public IndexWriteConfig text(String... fields) {
		for (String field : fields) {
			fieldIndexes.put(field, FieldIndex.TEXT) ;
		}
		return this ;
	}

	public IndexWriteConfig num(String... fields) {
		for (String field : fields) {
			fieldIndexes.put(field, FieldIndex.NUMBER) ;
		}
		return this ;
	}

	public IndexWriteConfig date(String... fields) {
		for (String field : fields) {
			fieldIndexes.put(field, FieldIndex.DATETIME) ;
		}
		return this ;
	}


	
	public PropertyId fieldIndexTo(PropertyId pid){
		pid.fieldIndex(ObjectUtil.coalesce(fieldIndexes.get(pid.idString()), FieldIndex.UNKNOWN)) ;
		
		return pid ;
	}

	public IndexWriteConfig ignoreBodyField(){
		this.ignoreBody = true ;
		return this ;
	}

	public boolean isIgnoreBodyField(){
		return ignoreBody ;
	}

}
