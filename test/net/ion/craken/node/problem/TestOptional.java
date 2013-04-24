package net.ion.craken.node.problem;

import java.util.Map;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

import com.google.common.base.Optional;

import junit.framework.TestCase;

public class TestOptional extends TestBaseCrud {

	public void testOf() throws Exception {
		Optional<Integer> option = Optional.of(1);
		
		Debug.line(option.asSet()) ;
		
		Object obj = null ;
		final Optional<Object> nullable = Optional.fromNullable(obj);
		Debug.line(nullable.isPresent(), nullable.orNull(), nullable.or(1), Optional.absent().isPresent()) ;
	}
	
	
	public void testOptionOf() throws Exception {
		
		RefMap rmap = new RefMap();
		
		Object get = rmap.property("key", new StringBuilder("1")).property("key") ;
		StringBuilder sb = (StringBuilder) get ;
		sb.append("2") ;
		
		Debug.line(rmap.property("key")) ;
	}
	
	public void testRef() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("name", Optional.of(new StringBuilder("bleujin")));
				return null ;
			}
		}).get() ;
		
		Optional<StringBuilder> optional = (Optional) session.pathBy("/bleujin").property("name") ;
		assertEquals("bleujin", optional.get().toString()) ;
		optional.get().append(" hi") ;
		
		Optional<StringBuilder> mod = (Optional) session.pathBy("/bleujin").property("name") ;
		Debug.line(mod.get()) ;
	}
}

class RefMap {
	private Map<String, Optional> map = MapUtil.newMap() ;

	public RefMap property(String key, Object value){
		map.put(key, Optional.of(value)) ;
		return this ;
	}
	public Object property(String key){
		return map.get(key).get() ;
	}
	
	
}