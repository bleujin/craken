package net.ion.craken.node.search;

import junit.framework.TestCase;
import net.ion.craken.node.crud.FunctionFilter;

public class TestFunctionFilter extends TestCase{

	public void testSimple() {
		new FunctionFilter("this.dummy >= 10").create() ;

		new FunctionFilter("this.dummy >= 10 and this.dummy < 20").create() ;
	}
	
	
	
}
