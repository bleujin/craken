package net.ion.craken.node.crud.tree.impl;

import java.io.Serializable;

import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.WriteDocument;

public class PropertyId implements Serializable {
	private static final long serialVersionUID = 7910617121345547495L;
	
	public static enum PType implements Serializable {
		NORMAL, REFER
	}

	private final PType type;
	private final String key;
	
	private PropertyId(PType type, String key){
		this.type = type ;
		this.key = key ;
	}
	
	public static final PropertyId normal(String key){
		return new PropertyId(PType.NORMAL, key) ;
	}

	public static final PropertyId refer(String key){
		return new PropertyId(PType.REFER, key) ;
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
		return "PropertyId[type="+type() + ",key=" + key + "]" ;
//		return ToStringBuilder.reflectionToString(this) ;
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
	
	public void indexTo(WriteDocument doc, IndexWriteConfig iwconfig, TreeNodeKey nodeKey, PropertyValue pvalue) {
		if (nodeKey.fqnString().startsWith("/__") && this.key.equals("tlogs")) return ;
		
		for (Object e : pvalue.asSet()) {
			if (e == null) continue ;
			FieldIndex fieldIndex = (type == PType.REFER) ?  FieldIndex.KEYWORD : iwconfig.fieldIndex(key) ; 
			
			switch(fieldIndex){
				case IGNORE :
					break ;
				case NUMBER :
					doc.number(idString(), NumberUtil.toLong(e.toString(), 0L)) ;
					break ;
				case TEXT :
					doc.text(idString(), e.toString()) ;
					break ;
				case DATETIME :
					doc.add(MyField.date(idString(), NumberUtil.toInt(StringUtil.substringBefore(e.toString(), "-")), NumberUtil.toInt(StringUtil.substringAfter(e.toString(), "-")))) ;
					break ;
				case KEYWORD :
					doc.keyword(idString(), ObjectUtil.toString(e)) ;
					break ;
				case UNKNOWN : {
					
					doc.unknown(idString(), e) ;
					break ;
				}
			}
		}
	}

	public boolean isSystemProperty() {
		return key.startsWith("__");
	}

}
