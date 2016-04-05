package net.ion.craken.tree;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestPropertyBytes extends TestBaseCrud{

	public void testWriteByte() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/bleujin").property("name", new ByteObject("HelloWorld".getBytes("UTF-8"))) ;
				return null;
			}
		}) ;
		
		
		Debug.line(session.pathBy("/emp/bleujin").property("name").asObject()) ;
	}
	
	public void testGrid() throws Exception {
		
	}
}
