package net.ion.script.rhino;

import junit.framework.TestCase;


public class TestReturnType extends TestCase {

	
	private RhinoEngine rengine;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.rengine = RhinoEngine.create().start().get() ;
	}
	
	public void testIntegerValue() {
		RhinoResponse response = rengine.newScript("IntegerScript").defineScript("var number = 100; number;").exec();
		assertEquals(response.getReturn(Integer.class), new Integer(100));
	}

	public void testDoubleValue() {
		RhinoResponse response = rengine.newScript("DoubleScript").defineScript("var number = 100.0; number.toFixed(2);").exec();
		assertEquals(Double.parseDouble(response.getReturn(String.class).toString()), 100.0d);
	}

	public void testStringValue() {
		RhinoResponse response = rengine.newScript("StringScript").defineScript("var str = 'stringValue'; str;").exec();
		assertEquals(response.getReturn(String.class).toString(), "stringValue");
	}
	
	public void testObject() throws Exception {
		RhinoResponse response = rengine.newScript("ObjectScript").bind("employee", new Employee("bleujin")).defineScript("var emp = employee; emp;").exec();
		assertEquals(response.getReturn(Employee.class).name(), "bleujin");
	}
	
	
}


class Employee {
	
	private String name ;
	public Employee(String name){
		this.name = name ;
	}
	public String name(){
		return name ;
	}
}