package net.ion.craken.node.crud;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.rows.FieldContext;
import net.ion.craken.node.convert.rows.FieldDefinition;
import net.ion.craken.node.convert.rows.FieldRender;

public class TestMultiRefChildren extends TestBaseCrud {
	

	
	public void testMultiRefChildren() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/afields/mfield").refTos("category", "/category/bleujin", "/category/hero", "/category/jin").property("id", "mfield").refTos("lower", "/afields/m1", "/afields/m2") ;
				wsession.pathBy("/afields/yfield").refTos("category", "/category/bleujin", "/category/jin").property("id", "yfield") ;
				wsession.pathBy("/afields/m1").property("id", "m1").refTos("lower", "/afields/m3") ;
				wsession.pathBy("/afields/m2").property("id", "m2") ;
				wsession.pathBy("/afields/m3").property("id", "m3") ;
				return null;
			}
		}) ;
		
		WalkRefChildren children = session.ghostBy("/afields").childTermQuery("@category", "/category/bleujin", false).find().walkRefChildren("lower") ;
		children.toAdRows("id", new FieldDefinition("lvl", new FieldRender<Integer>() {
			@Override
			public Integer render(FieldContext fcontext, ReadNode current) {
				WalkReadNode self = (WalkReadNode) current ;
				return self.level();
			}
		}), new FieldDefinition("parent", new FieldRender<String>() {
			@Override
			public String render(FieldContext fcontext, ReadNode current) {
				WalkReadNode self = (WalkReadNode) current ;
				return self.from().fqn().name();
			}
		})).debugPrint(); ;
	}
	

}
