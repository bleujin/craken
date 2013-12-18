package net.ion.script;

import junit.framework.TestCase;
import net.ion.script.rhino.RhinoEngine;

public class TestPreDefineScript extends TestCase{

	protected RhinoEngine rengine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.rengine = RhinoEngine.create() ;
		rengine.preDefineScript("hello", "function sayHello(name){return 'Hello ' + name};") ;
		
		rengine.start().get();
	}

	@Override
	protected void tearDown() throws Exception {
		rengine.shutdown();
		super.tearDown();
	}

	public void testPreDefineFunction() throws Exception {
		assertEquals("Hello bleujin", rengine.newScript("helloworld").defineScript("sayHello('bleujin');").exec().getReturn(String.class)) ;
	}
	
	public void testIllgalStateAfterStart() throws Exception {
		try {
			rengine.preDefineScript("hello", "function hiHello(name){return 'Hello ' + name};") ;
			fail() ;
		} catch(IllegalStateException expect){
		}
	}
	
}
