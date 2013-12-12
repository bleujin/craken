package net.ion.script.rhino;

import junit.framework.TestCase;

public class TestBaseScript extends TestCase{

	public TestBaseScript(){
		super() ;
	}
	public TestBaseScript(String name){
		super(name) ;
	}
	
	protected RhinoEngine rengine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.rengine = RhinoEngine.create().start();
	}
	
	@Override
	protected void tearDown() throws Exception {
		rengine.close() ;
		super.tearDown();
	}
	

}
