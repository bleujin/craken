package net.ion.craken.expression;

import java.lang.reflect.Method;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

import org.apache.commons.beanutils.MethodUtils;

public class TestParser extends TestBaseCrud {

	public void testWhere() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20);
				return null;
			}
		});

		assertEquals(1, session.root().children().where("age >= 20").where("name = 'bleujin'").toList().size());
		assertEquals(1, session.root().children().where("age >= 20 and name = 'bleujin'").toList().size());
		assertEquals(1, session.root().children().where("(case when this.name = 'bleujin' then this.age else 0 end) > 0 ").toList().size());
	}

	
	public void testInvoke() throws Exception {

		Object ref = new MyRef() ;
		
		
		Method[] mts = ref.getClass().getMethods();

		Debug.line(Number.class.isAssignableFrom(int.class), Number.class.isAssignableFrom(Integer.class)) ;

		
		Object result = MethodUtils.invokeMethod(ref, "add", 3);
		Debug.line(result) ;
	}
	
}


class MyRef {
	private int sum = 0 ;
	
	public int add(Integer a){
		this.sum = sum + a;
		return sum ;
	}
}