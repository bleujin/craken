package net.ion.script.rhino;

import junit.framework.TestCase;

// https://developer.mozilla.org/en-US/docs/Rhino_documentation
public class TestBaseScript extends TestCase {

	public TestBaseScript() {
		super();
	}

	public TestBaseScript(String name) {
		super(name);
	}

	protected RhinoEngine rengine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.rengine = RhinoEngine.create().start().get();
	}

	@Override
	protected void tearDown() throws Exception {
		rengine.shutdown();
		super.tearDown();
	}

}
