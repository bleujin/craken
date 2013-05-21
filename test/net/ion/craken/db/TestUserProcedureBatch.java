package net.ion.craken.db;

import java.sql.SQLException;

import net.ion.craken.node.exception.AlreadyExistsException;
import net.ion.framework.db.Rows;
import net.ion.framework.db.procedure.IUserProcedureBatch;
import net.ion.framework.db.procedure.IUserProcedures;

public class TestUserProcedureBatch extends TestBaseProcedure {

	
	public void testCreateUserProcedures() throws Exception {
		IUserProcedureBatch upt = dc.createUserProcedureBatch("dummy@batchWith(?,?,?)");
		upt.addParam(new String[]{"bleujin", "hero", "jin"})
			.addParam(new int[]{20, 30, 40})
			.addParam(new String[]{"seoul", "busan", "inchon"}).execUpdate() ;
		
		
		Rows rows = dc.createUserProcedure("dummy@listPersonBy()").execQuery();
		rows.debugPrint();
	}
	

	public void testConvertPrimitive() throws Exception {
		IUserProcedureBatch upt = dc.createUserProcedureBatch("dummy@batchWith(?,?,?)");
		upt.addParam(new String[]{"bleujin", "hero", "jin"})
			.addParam(new Integer[]{20, 30, 40})
			.addParam(new String[]{"seoul", "busan", "inchon"}).execUpdate() ;
		
		
		Rows rows = dc.createUserProcedure("dummy@listPersonBy()").execQuery();
		rows.debugPrint();
	}
	

	
	public void testTransaction() throws Exception {
		IUserProcedureBatch upt = dc.createUserProcedureBatch("dummy@batchWith(?,?,?)");
		upt.addParam(new String[]{"bleujin", "hero", "jin"})
			.addParam(new int[]{20, 30, 40})
			.addParam(new String[]{"seoul", "busan", "inchon"}).execUpdate() ;
		
		try {
			upt.execUpdate() ;
			fail() ;
		} catch(SQLException expect){
			
		}
		
	}

}
