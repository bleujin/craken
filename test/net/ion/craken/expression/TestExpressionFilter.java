package net.ion.craken.expression;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestExpressionFilter extends TestBaseCrud {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20).child("address").property("city", "seoul") ;
				wsession.pathBy("/bleujin").refTo("friend", "/hero") ;
				return null;
			}
		}) ;
	}
	
	public void testEq() throws Exception {
		assertEquals(1, session.root().children().where("this.name = 'bleujin'").toList().size()) ;
		assertEquals(1, session.root().children().where("this.age = 20").toList().size()) ;

		assertEquals(0, session.root().children().where("this.name = 'jin'").toList().size()) ;
		assertEquals(0, session.root().children().where("this.age = 21").toList().size()) ;

		assertEquals(0, session.root().children().where("this.age = '21'").toList().size()) ;
	}
	
	public void testGt() throws Exception {
		assertEquals(1, session.root().children().where("this.name > 'bleu'").toList().size()) ;
		assertEquals(1, session.root().children().where("this.age > 19").toList().size()) ;

		assertEquals(0, session.root().children().where("this.name > 'bleujin'").toList().size()) ;
		assertEquals(0, session.root().children().where("this.age > 20").toList().size()) ;

		assertEquals(0, session.root().children().where("this.age > '20'").toList().size()) ;

	}
	
	public void testGte() throws Exception {
		assertEquals(1, session.root().children().where("this.name > 'bleu'").toList().size()) ;
		assertEquals(1, session.root().children().where("this.age > 19").toList().size()) ;

		assertEquals(1, session.root().children().where("this.name >= 'bleujin'").toList().size()) ;
		assertEquals(1, session.root().children().where("this.age >= 20").toList().size()) ;
	}
	
	public void testNot() throws Exception {
		assertEquals(0, session.root().children().where("not (this.name > 'bleu')").toList().size()) ;
		assertEquals(1, session.root().children().where("not (this.name < 'bleu')").toList().size()) ;
	}
	
	
	public void testNeg() throws Exception {
		assertEquals(1, session.root().children().where("20 > -this.age").toList().size()) ;
		assertEquals(1, session.root().children().where("10 < -(-this.age)").toList().size()) ;
	} 
	
	
	public void testLike() throws Exception {
		assertEquals(1, session.root().children().where("this.name like '%bleu%'").toList().size()) ;
		assertEquals(1, session.root().children().where("this.name like 'bleu%'").toList().size()) ;
		assertEquals(0, session.root().children().where("this.name like 'bled%'").toList().size()) ;
	}
	
	public void testIsNull() throws Exception {
		assertEquals(0, session.root().children().where("this.name is null").toList().size()) ;
		assertEquals(1, session.root().children().where("not (this.name is null)").toList().size()) ;
		assertEquals(1, session.root().children().where("this.fname is null").toList().size()) ;
	}


	public void testBetween() throws Exception {
		assertEquals(0, session.root().children().where("this.age between 10 and 19").toList().size()) ;
		assertEquals(1, session.root().children().where("this.age between 10 and 20").toList().size()) ;
		assertEquals(1, session.root().children().where("this.age between 20 and 30").toList().size()) ;
		assertEquals(0, session.root().children().where("this.age between 21 and 30").toList().size()) ;
	}
	
	
	public void testIn() throws Exception {
		assertEquals(1, session.root().children().where("this.age in (20, 21, 22)").toList().size()) ;
		assertEquals(0, session.root().children().where("this.age in (21, 22)").toList().size()) ;

		assertEquals(1, session.root().children().where("this.name in ('bleujin', 'hero')").toList().size()) ;
		assertEquals(1, session.root().children().where("'bleujin' in (this.name, this.age)").toList().size()) ;
		assertEquals(0, session.root().children().where("'hero' in (this.name, this.age)").toList().size()) ;
	}
	
	public void testFunction() throws Exception {
		assertEquals(1, session.root().children().where("startsWith(this.name, 'bleu') = true").toList().size()) ;
		assertEquals(1, session.root().children().where("indexOf(this.name, 'bleu') = 0").toList().size()) ;
	}
	
	public void testNumOper() throws Exception {
		assertEquals(1, session.root().children().where("1 - 1 = 0").toList().size()) ;
		assertEquals(1, session.root().children().where("1 * 1 = 1").toList().size()) ;
		assertEquals(1, session.root().children().where("1 / 1 = 1").toList().size()) ;
		assertEquals(1, session.root().children().where("1 % 1 = 0").toList().size()) ;
		try {
			assertEquals(1, session.root().children().where("1 * 'a' = 1").toList().size()) ;
			fail() ;
		} catch(ArithmeticException expect){}

	}
	
	
//	public void testWildCard() throws Exception {
//		assertEquals(1, session.root().children().where("this.* = 'bleu'").toList().size()) ;
//		assertEquals(0, session.root().children().where("this.* = 'jin'").toList().size()) ;
//	}

	
	public void testQualifiedName() throws Exception {
		assertEquals(1, session.root().children().where("this.address.city = 'seoul'").toList().size()) ;
		assertEquals(1, session.root().children().where("address.city = 'seoul'").toList().size()) ;
		assertEquals(0, session.root().children().where("address.city = 'busan'").toList().size()) ;
	}
	
	
	public void testSimpleCaseWhen() throws Exception {
		assertEquals(1, session.root().children().where("case this.name when 'bleujin' then 'self' when 'hero' then 'self' else 'other' end = 'self'").toList().size()) ;
	}
	
	public void testFullCaseWhen() throws Exception {
		assertEquals(1, session.root().children().where("case when (this.age >= 20) then this.name else 'hello' end = 'bleujin'").toList().size()) ;
	}
	
	
	public void testAnd() throws Exception {
		assertEquals(0, session.root().children().where("this.name = 'bleujin' and age > 20").toList().size()) ;
		assertEquals(1, session.root().children().where("this.name = 'bleujin' and age >= 20").toList().size()) ;
	}

	public void testOr() throws Exception {
		assertEquals(1, session.root().children().where("this.name = 'bleujin' or age > 20").toList().size()) ;
		assertEquals(1, session.root().children().where("this.name = 'hero' or age <= 20").toList().size()) ;
	}

	
	
}
