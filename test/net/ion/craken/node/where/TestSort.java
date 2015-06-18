package net.ion.craken.node.where;

import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.SearchResponse;

public class TestSort extends TestBaseCrud {

	@Override
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
//				wsession.iwconfig().num("num") ;
				
				for (int i = 0; i < 20; i++) {
					WriteNode node = wsession.pathBy("/" + i).property("num", i) ; //.property("snum", "" + i).property("odd", i % 2 == 0) ;
					// if ( i % 2 == 0) node.property("odded", 1) ;
				}
				return null;
			}
		}) ;
	}

	public void testSort() throws Exception {
		session.root().childQuery("").where("").ascending("num").find().debugPrint();
	}
	
	
	
	public void testDesc() throws Exception {
		Sort sort = session.root().childQuery("").where("").sort("num=desc").sort() ;
		
		SortField sfield = sort.getSort()[0];
		Debug.line(sfield.getType(), sfield.getField(), sfield.getReverse());
		assertEquals(true, sfield.getType() == Type.STRING) ;
		assertEquals(true, sfield.getReverse()) ;
		assertEquals("num", sfield.getField()) ;
		
		sort = session.root().childQuery("").where("").sort("num=desc").find().request().sort() ;
		sfield = sort.getSort()[0];
		Debug.line(sfield.getType(), sfield.getField(), sfield.getReverse());
		assertEquals(true, sfield.getType() == Type.STRING) ;
		assertEquals(true, sfield.getReverse()) ;
		assertEquals("num", sfield.getField()) ;

	}

	public void testAsc() throws Exception {
		Sort sort = session.root().childQuery("").where("").sort("num=asc").sort() ;
		
		SortField sfield = sort.getSort()[0];
		Debug.line(sfield.getType(), sfield.getField(), sfield.getReverse());
		assertEquals(true, sfield.getType() == Type.STRING) ;
		assertEquals(false, sfield.getReverse()) ;
		assertEquals("num", sfield.getField()) ;

		sort = session.root().childQuery("").where("").sort("num=asc").find().request().sort() ;
		sfield = sort.getSort()[0];
		Debug.line(sfield.getType(), sfield.getField(), sfield.getReverse());
		assertEquals(true, sfield.getType() == Type.STRING) ;
		assertEquals(false, sfield.getReverse()) ;
		assertEquals("num", sfield.getField()) ;

		
	}
	

	
	public void testDescending() throws Exception {
		
		session.root().childQuery("").sort("num").find().debugPrint();
		
		assertEquals(10, session.root().childQuery("").ascendingNum("num").offset(1).skip(10).find().iterator().next().property("num").intValue(0));
		assertEquals(9, session.root().childQuery("").descendingNum("num").offset(1).skip(10).find().iterator().next().property("num").intValue(0));
	}
	


	public void testPrint() throws Exception {
		
		Central central = session.workspace().central() ;
		SearchResponse response = central.newSearcher().createRequest("").descendingNum("num").find();
		response.debugPrint("num");
		
		Debug.line(response.request().sort()) ;
		
//		session.root().childQuery("").sort("num=asc").find().debugPrint("num");
//		Debug.line();
//		session.root().childQuery("").sort("num=desc").find().debugPrint("num");

	}
	
	
	
	
}
