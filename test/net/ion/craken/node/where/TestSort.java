package net.ion.craken.node.where;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestSort extends TestBaseCrud {

	@Override
	protected void setUp() throws Exception {
		// TODO Auto-generated method stub
		super.setUp();
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 20; i++) {
					WriteNode node = wsession.pathBy("/" + i).property("num", i).property("odd", i % 2 == 0) ;
					if ( i % 2 == 0) node.property("odded", 1) ;
				}
				return null;
			}
		}) ;
	}

	public void testDescending() throws Exception {
		assertEquals(10, session.root().childQuery("").where("").sort("num").offset(1).skip(10).find().iterator().next().property("num").intValue(0));
		assertEquals(9, session.root().childQuery("").where("").sort("num=desc").offset(1).skip(10).find().iterator().next().property("num").intValue(0));
	}
	
}
