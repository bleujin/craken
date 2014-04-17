package net.ion.craken.node.crud;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;

public class TestPredicate extends TestBaseCrud{

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("string", "bleujin").property("int", 20).property("boolean", true).property("char", 'c').property("long", 2L)
					.property("arrayi", new int[]{1,2,3}).property("arrays", new String[]{"1","2","3"}).property("null", null);
				return null;
			}
		}) ;
	}
	
	public void testEq() throws Exception {

		assertEquals(1, session.root().children().eq("string", "bleujin").toList().size()) ;
		assertEquals(1, session.root().children().eq("int", 20).toList().size()) ;
		assertEquals(1, session.root().children().eq("boolean", true).toList().size()) ;
		assertEquals(1, session.root().children().eq("char", 'c').toList().size()) ;
		assertEquals(1, session.root().children().eq("long", 2L).toList().size()) ;

		assertEquals(0, session.root().children().eq("string", "Bleujin").toList().size()) ;
		assertEquals(0, session.root().children().eq("int", 30).toList().size()) ;
		assertEquals(0, session.root().children().eq("boolean", false).toList().size()) ;
		assertEquals(0, session.root().children().eq("char", 'd').toList().size()) ;
		assertEquals(0, session.root().children().eq("long", 2).toList().size()) ;
	}
	
	public void testAll() throws Exception {
		assertEquals(1, session.root().children().all("arrayi", new Integer[]{1,2,3}).toList().size()) ;
		assertEquals(1, session.root().children().all("arrayi", new int[]{1,2,3}).toList().size()) ;

		assertEquals(0, session.root().children().all("arrayi", new int[]{1,2,3,4}).toList().size()) ;
		assertEquals(0, session.root().children().all("arrayi", new int[]{1,3,2}).toList().size()) ;

		
		assertEquals(1, session.root().children().all("arrays", new String[]{"1","2","3"}).toList().size()) ;
		assertEquals(0, session.root().children().all("arrays", new String[]{"1","2","3","4"}).toList().size()) ;
	}
	

	public void testGt() throws Exception {
		assertEquals(1, session.root().children().gt("int", 19).toList().size()) ;
		assertEquals(0, session.root().children().gt("int", 20).toList().size()) ;
		assertEquals(0, session.root().children().gt("int", 21).toList().size()) ;

		assertEquals(1, session.root().children().gt("long", 1L).toList().size()) ;
		assertEquals(0, session.root().children().gt("long", 2L).toList().size()) ;
		assertEquals(0, session.root().children().gt("long", 3L).toList().size()) ;

		assertEquals(1, session.root().children().gt("string", "bleuji").toList().size()) ;
		assertEquals(0, session.root().children().gt("string", "bleujin").toList().size()) ;
		assertEquals(0, session.root().children().gt("string", "c").toList().size()) ;

		// null
		assertEquals(0, session.root().children().gt("ddd", "bleuji").toList().size()) ;

	}

	public void testGte() throws Exception {
		assertEquals(1, session.root().children().gte("int", 19).toList().size()) ;
		assertEquals(1, session.root().children().gte("int", 20).toList().size()) ;
		assertEquals(0, session.root().children().gte("int", 21).toList().size()) ;

		assertEquals(1, session.root().children().gte("long", 1L).toList().size()) ;
		assertEquals(1, session.root().children().gte("long", 2L).toList().size()) ;
		assertEquals(0, session.root().children().gte("long", 3L).toList().size()) ;

		assertEquals(1, session.root().children().gte("string", "bleuji").toList().size()) ;
		assertEquals(1, session.root().children().gte("string", "bleujin").toList().size()) ;
		assertEquals(0, session.root().children().gte("string", "c").toList().size()) ;

		// null
		assertEquals(0, session.root().children().gte("ddd", "bleuji").toList().size()) ;

	}

	

	public void testLt() throws Exception {
		assertEquals(0, session.root().children().lt("int", 19).toList().size()) ;
		assertEquals(0, session.root().children().lt("int", 20).toList().size()) ;
		assertEquals(1, session.root().children().lt("int", 21).toList().size()) ;

		assertEquals(0, session.root().children().lt("long", 1L).toList().size()) ;
		assertEquals(0, session.root().children().lt("long", 2L).toList().size()) ;
		assertEquals(1, session.root().children().lt("long", 3L).toList().size()) ;

		assertEquals(0, session.root().children().lt("string", "bleuji").toList().size()) ;
		assertEquals(0, session.root().children().lt("string", "bleujin").toList().size()) ;
		assertEquals(1, session.root().children().lt("string", "c").toList().size()) ;

		// null
		assertEquals(0, session.root().children().lt("ddd", "bleuji").toList().size()) ;

	}

	public void testLte() throws Exception {
		assertEquals(0, session.root().children().lte("int", 19).toList().size()) ;
		assertEquals(1, session.root().children().lte("int", 20).toList().size()) ;
		assertEquals(1, session.root().children().lte("int", 21).toList().size()) ;

		assertEquals(0, session.root().children().lte("long", 1L).toList().size()) ;
		assertEquals(1, session.root().children().lte("long", 2L).toList().size()) ;
		assertEquals(1, session.root().children().lte("long", 3L).toList().size()) ;

		assertEquals(0, session.root().children().lte("string", "bleuji").toList().size()) ;
		assertEquals(1, session.root().children().lte("string", "bleujin").toList().size()) ;
		assertEquals(1, session.root().children().lte("string", "c").toList().size()) ;

		// null
		assertEquals(0, session.root().children().gt("ddd", "bleuji").toList().size()) ;
	}


	public void testNe() throws Exception {
		assertEquals(1, session.root().children().ne("int", 19).toList().size()) ;
		assertEquals(0, session.root().children().ne("int", 20).toList().size()) ;
		assertEquals(1, session.root().children().ne("int", 21).toList().size()) ;

		assertEquals(1, session.root().children().ne("long", 1L).toList().size()) ;
		assertEquals(0, session.root().children().ne("long", 2L).toList().size()) ;
		assertEquals(1, session.root().children().ne("long", 3L).toList().size()) ;

		assertEquals(1, session.root().children().ne("string", "bleuji").toList().size()) ;
		assertEquals(0, session.root().children().ne("string", "bleujin").toList().size()) ;
		assertEquals(1, session.root().children().ne("string", "c").toList().size()) ;

		// null
		assertEquals(0, session.root().children().gt("ddd", "bleuji").toList().size()) ;
	}

	public void testNotAll() throws Exception {
		assertEquals(1, session.root().children().notAll("int", 19).toList().size()) ;
		assertEquals(0, session.root().children().notAll("int", 20).toList().size()) ;
		assertEquals(1, session.root().children().notAll("int", 21).toList().size()) ;

		
		assertEquals(1, session.root().children().notAll("arrayi", 0).toList().size()) ;
		assertEquals(0, session.root().children().notAll("arrayi", 1).toList().size()) ;
		assertEquals(1, session.root().children().notAll("arrayi", 4).toList().size()) ;
	}

	public void testIn() throws Exception {
		assertEquals(0, session.root().children().in("string", "hero", "jin").toList().size()) ;
		assertEquals(1, session.root().children().in("string", "hero", "jin", "bleujin").toList().size()) ;
	}
	
	public void testAny() throws Exception {
		assertEquals(0, session.root().children().any("int", 19).toList().size()) ;
		assertEquals(1, session.root().children().any("int", 20).toList().size()) ;
		assertEquals(0, session.root().children().any("int", 21).toList().size()) ;


		assertEquals(0, session.root().children().any("arrayi", 0).toList().size()) ;
		assertEquals(1, session.root().children().any("arrayi", 1).toList().size()) ;
		assertEquals(0, session.root().children().any("arrayi", 4).toList().size()) ;
	}


	public void testContains() throws Exception {
		assertEquals(1, session.root().children().contains("string", "bleu").toList().size()) ;
		assertEquals(1, session.root().children().contains("string", "jin").toList().size()) ;
		assertEquals(0, session.root().children().contains("string", "bleuhero").toList().size()) ;
	}

	
	
	
	public void testExists() throws Exception {
		assertEquals(1, session.root().children().exists("string").toList().size()) ;
		assertEquals(1, session.root().children().exists("null").toList().size()) ;
		assertEquals(0, session.root().children().exists("nnnn").toList().size()) ;
	}
	
	
	public void testType() throws Exception {
		assertEquals(1, session.root().children().type("string", String.class).toList().size()) ;
		assertEquals(1, session.root().children().type("string", CharSequence.class).toList().size()) ;
		assertEquals(0, session.root().children().type("string", Integer.class).toList().size()) ;
	}
	
	

	public void testSize() throws Exception {
		assertEquals(1, session.root().children().size("string", 1).toList().size()) ;
		assertEquals(0, session.root().children().size("string", 2).toList().size()) ;
		
		assertEquals(1, session.root().children().size("arrayi", 3).toList().size()) ;
		assertEquals(0, session.root().children().size("arrayi", 2).toList().size()) ;
	}
	

	
	
	
}
