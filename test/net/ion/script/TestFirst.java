package net.ion.script;

import java.io.Reader;
import java.io.StringReader;

import net.ion.script.rhino.RhinoResponse;
import net.ion.script.rhino.TestBaseScript;

public class TestFirst extends TestBaseScript {

	public void testHelloWorld() throws Exception {
		RhinoResponse response = rengine.newScript("Hello").defineScript("print('Hello World!')").exec() ;
		
		assertEquals(true, response.isOk()) ;
	}
	
	
	public void testFromReader() throws Exception {
		Reader reader = new StringReader("print('Hello World!')") ;
		RhinoResponse response = rengine.newScript("fromFile").defineScript(reader).exec() ;
		
		assertEquals(true, response.isOk()) ;
	}
	
	
	

}
