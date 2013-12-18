package net.ion.script;

import net.ion.script.rhino.RhinoResponse;
import net.ion.script.rhino.TestBaseScript;

public class TestImportPackage extends TestBaseScript {

	public void testUtilMap() throws Exception {
		String script = "importPackage(java.util) ;" +
				"" +
				" var map = new HashMap() ;" +
				" map.put('name', 'bleujin') ;" +
				" map.get('name') ;" ;
		RhinoResponse response = rengine.newScript("import").defineScript(script).exec();
		assertEquals("bleujin", response.getReturn(String.class)) ;
	}
	

}
