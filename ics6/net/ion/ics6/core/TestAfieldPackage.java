package net.ion.ics6.core;

import net.ion.craken.db.CrakenScriptManager;

public class TestAfieldPackage extends TestBasePackage{

	
	public void testCreateWith() throws Exception {
		
		String afieldId = "address";
		String afieldNm = "HomeAddress";
		String grpCd = "dftgroup";
		String afieldExp = "User Home Address";
		String typeCd = "String";
		
		int aFieldLen = 0;
		boolean isMndt = false;
		int indexOption = 0 ; 
		int aFieldMaxLen = 0;
		int aFieldVLen = 0;
		String fileTypeCd = "ALL"; 
		int examId = 0;
		String defaultValue ="seoul" ; 
		
		int count = cs.execUpdate("afield@createWith", afieldId, afieldNm, grpCd, afieldExp, typeCd, aFieldLen, isMndt, indexOption, aFieldMaxLen, aFieldVLen, fileTypeCd, examId, defaultValue) ;
		
		assertEquals(1, count);
		
		assertEquals("HomeAddress", session.pathBy("/afields/address").property("afieldnm").asString());
	}
}
