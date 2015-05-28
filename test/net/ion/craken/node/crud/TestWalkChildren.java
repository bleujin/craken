package net.ion.craken.node.crud;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;

public class TestWalkChildren extends TestBaseCrud{

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/category/scat/dev").property("catid", "dev").property("catnm", "develop team");
				wsession.pathBy("/category/scat/sales").property("catid", "sales").property("catnm", "sales team");
				wsession.pathBy("/category/scat/research").property("catid", "reserch").property("deptno", "research team");
				wsession.pathBy("/category/scat/dev/cxm").property("catid", "cxm").property("catnm", "cxm team");
				
				wsession.pathBy("/article/cxm/7756").property("artid", 7756).property("catid", "cxm").property("priority", 1).property("userid", "bleujin").property("age", 20) ;
				wsession.pathBy("/article/cxm/7789").property("artid", 7789).property("catid", "cxm").property("priority", 2).property("userid", "hero").property("age", 21) ;
				wsession.pathBy("/article/cxm/7801").property("artid", 7801).property("catid", "cxm").property("priority", 3).property("userid", "airkjh").property("age", 22) ;
				wsession.pathBy("/article/dev/7905").property("artid", 7905).property("catid", "dev").property("priority", 1).property("userid", "novision").property("age", 23) ;
				return null;
			}
		}) ;
	}
	
	public void testWhere() throws Exception {
		assertEquals(1, session.root().walkChildren().where("age =20").count()) ;
	}
	

	public void testQueryWhere() throws Exception {
		session.root().childQuery("", true).where("age <> 20").find().debugPrint(); 
	}
	
	
	public void testQueryInclude() throws Exception {
		session.pathBy("/article").childQuery("catid:(dev cxm)", true).where("age >= 22").find().debugPrint(); 
	}
	
	
	
}
