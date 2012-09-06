package net.ion.craken;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TestEntry extends TestBase {

	
	public void testResave() throws Exception {
		
		final LegContainer<Employee> container = craken.defineLeg(Employee.class);
		
		ScheduledExecutorService es = Executors.newScheduledThreadPool(2);
		final Employee emp = container.newInstance(7756).name("bleujin").save() ;
		
		es.schedule(new Callable<Boolean>(){
			public Boolean call() throws Exception {
				emp.name("mod bleujin").remove() ;
				assertEquals(0, container.keySet().size()) ;
				return true;
			}
		}, 1, TimeUnit.SECONDS) ;
		
		es.schedule(new Callable<Boolean>(){
			public Boolean call() throws Exception {
				if (! container.containsKey(emp.key())){
					emp.name("mod bleujin").save() ;
				}
				assertEquals(1, container.keySet().size()) ;
				return true;
			}
		}, 2, TimeUnit.SECONDS) ;
		
		
		Thread.sleep(2500) ;
		assertEquals(1, container.keySet().size()) ;
		assertEquals("mod bleujin", container.findOne().name()) ;
		
	}
	
}
