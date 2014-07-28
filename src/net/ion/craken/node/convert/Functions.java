package net.ion.craken.node.convert;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.SelectProjection;
import net.ion.craken.expression.TerminalParser;
import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.convert.rows.AdNodeRows;
import net.ion.craken.node.crud.bean.ToBeanStrategy;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.db.Rows;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.rosetta.Parser;

import com.google.common.base.Function;

public class Functions {

	
	public final static Function<ReadNode, Rows> rowsFunction(final ReadSession session, final String expr){
		return new Function<ReadNode, Rows>(){
			@Override
			public Rows apply(ReadNode node) {
//				ColumnParser cparser = session.workspace().getAttribute(ColumnParser.class.getCanonicalName(), ColumnParser.class);
//				return CrakenNodeRows.create(session, ListUtil.toList(node).iterator() , cparser.parse(cols)) ;
				
				Parser<SelectProjection> parser = ExpressionParser.selectProjection();
				SelectProjection sp = TerminalParser.parse(parser, expr);
				return AdNodeRows.create(session, ListUtil.toList(node).iterator(), sp);
			}
		} ;
	}
	
	public final static <T> Function<ReadNode, T> beanCGIFunction(final Class<T> clz){
		return new Function<ReadNode, T>(){
			@Override
			public T apply(ReadNode node) {
				return ToBeanStrategy.ProxyByCGLib.toBean(node, clz) ;
			}
		} ;
	}

	public final static <T> Function<ReadNode, T> beanReflectionFunction(final Class<T> clz){
		return new Function<ReadNode, T>(){
			@Override
			public T apply(ReadNode node) {
				return ToBeanStrategy.EasyByJson.toBean(node, clz) ;
			}
		} ;
	}
	
	public static Function<ReadNode, Map> toPropertyValueMap(){
		return new Function<ReadNode, Map>(){
			@Override
			public Map apply(ReadNode node) {
				Map<String, Object> properties = MapUtil.newMap() ;
				for(Entry<PropertyId, PropertyValue> entry : node.toMap().entrySet()){
					final PropertyId pid = entry.getKey();
					final PropertyValue pvalue = entry.getValue();
					if (pid.type() == PropertyId.PType.NORMAL){
						properties.put(pid.getString(), pvalue.size() == 1 ? pvalue.value() : pvalue.asSet());
					}
				}
				return properties ;
			}
		} ;
	}
	
	public static final Function<ReadNode, Map<String, Object>> READ_TOFLATMAP = new Function<ReadNode, Map<String, Object>>(){
		@Override
		public Map<String, Object> apply(ReadNode target) {
			Map<String, Object> result = MapUtil.newMap() ;
			for(PropertyId pid : target.normalKeys()){
				result.put(pid.idString(), target.propertyId(pid).value()) ;
			}
			return result;
		}
	} ;
	

	public static final Function<WriteNode, Map<String, Object>> WRITE_TOFLATMAP =  new Function<WriteNode, Map<String, Object>>(){
		@Override
		public Map<String, Object> apply(WriteNode target) {
			Map<String, Object> result = MapUtil.newMap() ;
			for(PropertyId pid : target.normalKeys()){
				result.put(pid.idString(), target.propertyId(pid).value()) ;
			}
			return result;
		}
	} ;


	public static Function<ReadNode, JsonObject> toJson() {
		return new Function<ReadNode, JsonObject>(){
			@Override
			public JsonObject apply(ReadNode node) {
				JsonObject result = new JsonObject() ;
				
				Map<String, Object> properties = MapUtil.newMap() ;
				Map<String, Set> refs = MapUtil.newMap() ;
				for(Entry<PropertyId, PropertyValue> entry : node.toMap().entrySet()){
					if (entry.getKey().type() == PropertyId.PType.NORMAL){
						properties.put(entry.getKey().getString(), entry.getValue().asSet());
					} else {
						refs.put(entry.getKey().getString(), entry.getValue().asSet()) ;
					}
				}
				
				result.add("properties", JsonObject.fromObject(properties)) ;
				result.add("references", JsonObject.fromObject(refs)) ;
				result.add("children", JsonParser.fromObject(node.childrenNames())) ;
				
				return result ;
			}
		} ;
	}
	
	public static <T extends NodeCommon<? extends NodeCommon>> Function<T, JsonObject> toJsonExpression() {
		return new Function<T, JsonObject>(){
			@Override
			public JsonObject apply(T node) {
				JsonObject result = new JsonObject() ;
				
				Map<String, Object> properties = MapUtil.newMap() ;
				Map<String, Set> refs = MapUtil.newMap() ;
				for(Entry<PropertyId, PropertyValue> entry : node.toMap().entrySet()){
					if (entry.getKey().type() == PropertyId.PType.NORMAL){
						properties.put(entry.getKey().getString(), entry.getValue().asSet());
					} else {
						refs.put(entry.getKey().getString(), entry.getValue().asSet()) ;
					}
				}
				
				result.add("properties", JsonObject.fromObject(properties)) ;
				result.add("references", JsonObject.fromObject(refs)) ;
				
				return result ;
			}
		} ;
	}
	
	public final static Function<ReadNode, Void> READ_DEBUGPRINT = new Function<ReadNode, Void>() {
		@Override
		public Void apply(ReadNode target) {
			Debug.debug(target, target.keys());;
			return null ;
		}
	};


	public static final Function<Iterator<ReadNode>, JsonObject> CHILDLIST = new Function<Iterator<ReadNode>, JsonObject>(){
		@Override
		public JsonObject apply(Iterator<ReadNode> iter) {
			JsonObject result = new JsonObject() ;
			
			while(iter.hasNext()){
				ReadNode next = iter.next() ;
				result.add(next.fqn().name(), next.toValueJson());
			}
			return result;
		}
		
	};


}
