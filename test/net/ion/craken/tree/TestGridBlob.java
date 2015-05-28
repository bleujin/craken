package net.ion.craken.tree;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

public class TestGridBlob extends TestCase{

	
	public void testSerial() throws Exception {
		GridBlob gb = GridBlob.create(null, "/emp/bleujin.dat") ;
		
		Debug.line(gb.toJsonPrimitive());
	}
}
