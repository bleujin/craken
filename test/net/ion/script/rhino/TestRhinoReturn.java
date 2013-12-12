package net.ion.script.rhino;

import junit.framework.TestCase;


public class TestRhinoReturn extends TestCase {

	
	private RhinoEngine rengine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.rengine = RhinoEngine.create().start() ;
	}
	
	public void testReturnIntegerValueFormScript() {
		RhinoResponse response = rengine.newScript("Return Integer Value Script").defineScript("var number = 100; number;").exec();
		assertEquals((int) Double.parseDouble(response.getReturn(Double.class).toString()), 100);
	}

	public void testReturnDoubleValueFormScript() {
		RhinoResponse response = rengine.newScript("Return Double Value Script").defineScript("var number = 100.0; number.toFixed(2);").exec();
		assertEquals(Double.parseDouble(response.getReturn(String.class).toString()), 100.0d);
	}

	public void testReturnStringValueFormScript() {
		RhinoResponse response = rengine.newScript("Return String Value Script").defineScript("var str = 'stringValue'; str;").exec();
		assertEquals(response.getReturn(String.class).toString(), "stringValue");
	}
}
