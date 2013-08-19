package net.ion.craken.tree;

import java.io.Serializable;

import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.nsearcher.common.WriteDocument;

import org.apache.commons.lang.builder.ToStringBuilder;

public class PropertyId implements Serializable {
	private static final long serialVersionUID = 7910617121345547495L;
	
	public static enum PType implements Serializable {
		NORMAL, REFER
	}

	private final PType type;
	private final String key;
	private FieldIndex fieldIndex = FieldIndex.UNKNOWN ;
	
	private PropertyId(PType type, String key){
		this.type = type ;
		this.key = key ;
	}
	
	public static final PropertyId normal(String key){
		return new PropertyId(PType.NORMAL, key) ;
	}

	public static final PropertyId refer(String key){
		return new PropertyId(PType.REFER, key).fieldIndex(FieldIndex.KEYWORD) ;
	}
	
	public boolean equals(Object o){
		if (! (o instanceof PropertyId)) return false ;
		PropertyId that = (PropertyId) o ;
		return this.type == that.type && this.key.equals(that.key) ; 
	}
	
	public int hashCode(){
		return key.hashCode() + type.hashCode() ;
	}
	
	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}

	public String idString(){
		return (type == PType.REFER) ? "@" + key : key ;
	}
	
	public final static PropertyId fromIdString(String idString){
		return idString.startsWith("@") ? new PropertyId(PType.REFER, idString.substring(1)) : new PropertyId(PType.NORMAL, idString) ;
	}
	
	public String getString() {
		return key;
	}

	public PType type() {
		return type;
	}

	public PropertyId fieldIndex(FieldIndex fieldIndex) {
		this.fieldIndex = fieldIndex ;
		return this ;
	}
	
	public FieldIndex fieldIndex(){
		return fieldIndex ;
	}

	public void indexTo(WriteDocument doc, PropertyValue pvalue) {
		for (Object e : pvalue.asSet()) {
			if (e == null) continue ;
			switch(fieldIndex){
				case IGNORE :
					break ;
				case NUMBER :
					doc.number(idString(), NumberUtil.toLong(e.toString(), 0L)) ;
					break ;
				case TEXT :
					doc.text(idString(), e.toString()) ;
					break ;
				case KEYWORD :
					doc.keyword(idString(), ObjectUtil.toString(e)) ;
					break ;
				case UNKNOWN :
					doc.unknown(idString(), e) ;
					break ;
			}
		}
	}
}
