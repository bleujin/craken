package net.ion.craken.node.script;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.script.CrakenScript;
import net.ion.framework.db.Rows;

public class TestCrakenScript extends TestCase {

	private CrakenScript cs;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Craken r = Craken.inmemoryCreateWithTest() ;
		this.session = r.start().login("test") ;
		
		this.cs = CrakenScript.create(session, Executors.newScheduledThreadPool(1)) ;
		cs.readDir(new File("./test/net/ion/bleujin/script")) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		session.workspace().repository().shutdown() ;
		super.tearDown();
	}
	
	public void testReadPackage() throws Exception {
		assertEquals(2, cs.packages().size()) ;
	}
	
	public void testExecUpdate() throws Exception {
		int result = cs.execUpdate("afield@createWith", "bleujin", "created") ;
		assertEquals(1, result);
	}
	
	public void testSelect() throws Exception {
		int result = cs.execUpdate("afield@createWith", "rday", "registerDay") ;
		Rows rows = cs.execQuery("afield@listBy", 0, 2) ;
		
		rows.debugPrint(); 
	}
	
	
//	this.jsonBy = function(path, props) {
//		var found = session.pathBy(path) ;
//		return jbuilder.newInner().property(found, props).property("extra", "extravalue").buildRows("afieldId, afieldNm, extra") ; 
//	}
	public void testWhenJson() throws Exception {
		int result = cs.execUpdate("afield@batchWith", new String[]{"name", "rday"}, new String[]{"user name", "registerDay"}) ;
		
		Rows rows = cs.execQuery("afield@jsonBy", "/afields/name", "afieldId, afieldNm") ;
		rows.debugPrint();
		
		rows.first() ;
		assertEquals("extravalue", rows.getString("extra"));
		assertEquals("name", rows.getString("afieldId"));
		assertEquals("user name", rows.getString("afieldNm"));

		assertEquals("name", rows.getString(1));
		assertEquals("user name", rows.getString(2));
		assertEquals("extravalue", rows.getString(3));
	}
	
	
	public void testBatchUpdate() throws Exception {
		int result = cs.execUpdate("afield@batchWith", new String[]{"name", "rday"}, new String[]{"user name", "registerDay"}) ;
		assertEquals(2, result);
		
		Rows rows = cs.execQuery("afield@listBy", 0, 2) ;
		assertEquals(2, rows.getRowCount());
	}
	
	
	public void xtestDetect() throws Exception {
		cs.readDir(new File("./test/net/ion/bleujin/script"), true) ;
		while(true){
			testSelect();
			Thread.sleep(1000);
		}
		
	}

    public void testDBFunction() throws IOException, SQLException {
        cs.readDir(new File("./test/net/ion/airkjh/script"), false);


    }
	
	
	
}
