package net.ion.craken.db;

import java.io.File;
import java.util.concurrent.Executors;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.db.DBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.procedure.IUserProcedureBatch;
import net.ion.framework.db.servant.StdOutServant;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestCrakenScriptManager extends TestCase {

	protected DBController dc;
	private RepositoryImpl r;
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
		this.session = r.start().login("test") ;
		
		CrakenScriptManager dbm = CrakenScriptManager.create(session, Executors.newScheduledThreadPool(1), new File("./test/net/ion/bleujin/script")) ;
		this.dc = new DBController("craken", dbm, new StdOutServant());
		dc.initSelf() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		dc.destroySelf();
		r.shutdown() ;
	}
	
	
	public void testCreateUserProcedure() throws Exception {
		int result = dc.createUserProcedure("afield@createWith(?,?)").addParam("rday").addParam("registerDay").execUpdate() ;
		Debug.line(result);
		
		Rows rows = dc.createUserProcedure("afield@listBy(?,?)").addParam(0).addParam(2).execQuery() ;
		rows.debugPrint(); 
	}
	
	public void testCreateUserProcedureBatch() throws Exception {
		IUserProcedureBatch bat = dc.createUserProcedureBatch("afield@batchWith(?,?)") ;
		bat.addBatchParam(0, "rday") ;
		bat.addBatchParam(0, "registerday");
		
		bat.addBatchParam(1, "cday") ;
		bat.addBatchParam(1, "createday");
		int result = bat.execUpdate() ;
		
		assertEquals(2, result);
		Rows rows = dc.createUserProcedure("afield@listBy(?,?)").addParam(1).addParam(2).execQuery() ;
		rows.debugPrint(); 
	}
	
}
