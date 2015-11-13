package net.ion.craken.node.convert.rows;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.db.Page;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;

import com.google.common.base.Predicate;

public class TestAdRows extends TestBaseCrud{

	public void testPage() throws Exception {
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 25; i++) {
					String afieldId = RandomUtil.nextRandomString(10);
					wsession.pathBy("/afields", afieldId).property("afieldid", afieldId).property("index", i) ;
				}
				return null;
			}
		}) ;
		
		
		Rows rows = session.pathBy("/afields").children().filter(new Predicate<ReadNode>(){
			public boolean apply(ReadNode rnode) {
				return rnode.property("index").asInt() % 2 == 0;
			}
		}).ascending("index").toAdRows(Page.create(5, 2), "afieldid aid, index");
		rows.debugPrint();
		
		rows.first() ;
		Debug.line(rows.getString("cnt"));
	}
	
}
