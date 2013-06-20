package net.ion.craken.db;

import net.ion.framework.db.Rows;
import net.ion.framework.db.procedure.IUserProcedures;

public class TestUserProcedures extends TestBaseProcedure {

	
	
	
	public void testCreateUserProcedures() throws Exception {
		IUserProcedures upts = dc.createUserProcedures("addJob");
		upts.add(dc.createUserProcedure("dummy@addPersonWITH(?,?,?)").addParam("bleujin").addParam(20).addParam("seoul"));
		upts.add(dc.createUserProcedure("dummy@addPersonWITH(?,?,?)").addParam("hero").addParam(20).addParam("busan"));
		int result = upts.execUpdate();

		Rows rows = dc.createUserProcedure("dummy@listPersonBy()").execQuery();
		rows.debugPrint();

	}

}
