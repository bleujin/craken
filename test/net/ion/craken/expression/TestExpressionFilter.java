package net.ion.craken.expression;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.rosetta.Parser;

public class TestExpressionFilter extends TestBaseCrud {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20).addChild("address").property("city", "seoul") ;
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

	}
	
	public void testFunction() throws Exception {
		assertEquals(1, session.root().children().where("startsWith(this.name, 'bleu') = true").toList().size()) ;
		assertEquals(1, session.root().children().where("indexOf(this.name, 'bleu') = 0").toList().size()) ;
	}
	
	public void testWildCard() throws Exception {
		assertEquals(1, session.root().children().where("this.* = 'bleu'").toList().size()) ;
		assertEquals(0, session.root().children().where("this.* = 'jin'").toList().size()) ;
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
