package net.ion.craken;

import java.util.List;

import net.ion.framework.util.ListUtil;

public class TestModify extends TestBase {

	private LegContainer<Employee> leg  ; 

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		leg = craken.defineLeg(Employee.class);

		List<String> names = ListUtil.toList("bleujin", "hero", "novi", "iihi", "pm") ;
		for (int i = 0; i < 5; i++) {
			leg.newInstance(1000 + i).name(names.get(names.size() % (i+1))).age(20 + i).save() ;
		}
	}
	
	
	public void testRemove() throws Exception {
		assertEquals(5, leg.keySet().size()) ;
		leg.findByKey(1000).remove() ;
		
		assertEquals(4, leg.keySet().size()) ;
	}

	public void testModify() throws Exception {
		assertEquals("bleujin", leg.findByKey(1000).name()) ;

		leg.findByKey(1000).name("mod").save() ;
		
		assertEquals("mod", leg.findByKey(1000).name()) ;
	}
	
	public void testOverwrite() throws Exception {
		leg.newInstance(1000).age(25).save() ;
		
		assertEquals(true, leg.findByKey(1000).name() == null) ;
		assertEquals(true, leg.findByKey(1000).age() == 25) ;
	}
	
	public void testNotFound() throws Exception {
		assertEquals(true, leg.findByKey(3000) == null) ; 
	}
}
