package net.ion.craken.tree;

import net.ion.framework.parse.gson.JsonPrimitive;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestGridBlob extends TestCase{

	
	public void testSerial() throws Exception {
		GridBlob gb = GridBlob.create(null, "/emp/bleujin.dat") ;
		
		Debug.line(gb.toJsonPrimitive());
	}
}
