package net.ion.craken.expression;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestQualifiedName extends TestBaseCrud{
 
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20).child("address").property("city", "seoul") ;
				wsession.pathBy("/bleujin").refTo("dev", "/dept/dev").refTo("friend", "/hero") ;
				
				wsession.pathBy("/dept/dev").property("name", "dev") ;
				return null;
			}
		}) ;
	}

	public void testDefault() throws Exception {
		QualifiedNameExpression qe = QualifiedNameExpression.of("this", "name");
		assertEquals("bleujin", qe.value(session.pathBy("/bleujin")));
	}

	public void testExceptThis() throws Exception {
		QualifiedNameExpression qe = QualifiedNameExpression.of("name");
		assertEquals("bleujin", qe.value(session.pathBy("/bleujin")));
	}
	
	public void testChild() throws Exception {
		QualifiedNameExpression qe = QualifiedNameExpression.of("this", "address", "city");
		assertEquals("seoul", qe.value(session.pathBy("/bleujin")));
	}

	public void testRelation() throws Exception {
		QualifiedNameExpression qe = QualifiedNameExpression.of("this", "dev", "name");
		assertEquals("dev", qe.value(session.pathBy("/bleujin")));
	}
	

	public void testRelationButNotExist() throws Exception {
		QualifiedNameExpression qe = QualifiedNameExpression.of("this", "friend", "name"); // not exists
		assertEquals(true, qe.value(session.pathBy("/bleujin")) == null);
	}
	
	public void testHasNotRelation() throws Exception {
		QualifiedNameExpression qe = QualifiedNameExpression.of("this", "partner", "name"); // not exists
		assertEquals(true, qe.value(session.pathBy("/bleujin")) == null);
	}
}
