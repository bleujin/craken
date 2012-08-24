package net.ion.craken;

import java.util.List;

import net.ion.framework.db.Page;
import net.ion.framework.util.ListUtil;

public class TestFind extends TestBase {

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
	
	public void testFindByKey() throws Exception {
		Employee emp = leg.findByKey(1000);
		assertEquals(20, emp.age()) ;
	}
	
	public void testFindOne() throws Exception {
		Employee emp = leg.findOne(new EntryFilter<Employee>() {
			@Override
			public boolean filter(Employee entry) {
				return entry.age() > 22;
			}
		}) ;
		assertEquals(true, emp.age() > 22) ;
	}
	
	public void testFindPage() throws Exception {
		List<Employee> emps = leg.find(new EntryFilter<Employee>() {
			@Override
			public boolean filter(Employee entry) {
				return entry.age() > 22;
			}
		}) ;
		assertEquals(true, emps.size() == 2) ;
		
		emps = leg.find(new EntryFilter<Employee>() {
			@Override
			public boolean filter(Employee entry) {
				return entry.age() > 22;
			}
		}, Page.create(1, 2)) ;
		assertEquals(true, emps.size() == 1) ;
	}
	
	
}
