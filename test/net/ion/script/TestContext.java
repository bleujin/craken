package net.ion.script;

import net.ion.script.rhino.TestBaseScript;

public class TestContext extends TestBaseScript{

	
	public void testHello() throws Exception {
		rengine.newScript("hello").defineScript("print('Hello World');").exec() ;
	}
}
