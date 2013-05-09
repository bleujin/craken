package net.ion.craken.node.crud;

import com.google.common.base.Function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.CrakenNodeRows;
import net.ion.framework.db.Rows;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

public class TestTransformer extends TestBaseCrud {

	private ReadNode readnode;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/users/bleujin").property("name", "bleujin").property("age", 20) ;
				return null;
			}
		}) ;
		this.readnode = session.pathBy("/users/bleujin");
	}
	
	public void testFirst() throws Exception {
		Integer value = readnode.transformer(new Function<ReadNode, Integer>(){
			@Override
			public Integer apply(ReadNode node) {
				return node.property("age").value(0);
			}
		});
		
		assertEquals(20, value.intValue()) ;
	}
	
	public void testToRowsFunction() throws Exception {
		Rows rows = readnode.toRows("name", "age");
		
		
		assertEquals(20, rows.firstRow().getInt("age")) ;
		assertEquals("bleujin", rows.firstRow().getString("name")) ;
	}
}



