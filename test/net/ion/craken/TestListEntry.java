package net.ion.craken;

import java.util.List;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

import com.sun.jndi.ldap.EntryChangeResponseControl;

import net.ion.craken.simple.EmanonKey;
import net.ion.craken.simple.SimpleKeyFactory;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.ListUtil;

public class TestListEntry extends TestBase {

	public void testFristServer() throws Exception {
		LegContainer<EmployeeListEntry> emps = craken.defineLeg(EmployeeListEntry.class);
		
		EmployeeListEntry entry = emps.newInstance("dep").save() ;
		for (int i = 0; i < 10; i++) {
			entry.addEmployee(new Employee(1).age(20).name("bleujin")).save() ;
			Thread.sleep(1000) ;
			System.out.print('.') ;
		}
	}
	
	public void testSecondServer() throws Exception {
		LegContainer<EmployeeListEntry> emps = craken.defineLeg(EmployeeListEntry.class);
		emps.addListener(new EmpListener()) ;
		
		new InfinityThread().startNJoin() ;
	}
	
	@Listener
	public class EmpListener{
		@CacheEntryModified
		public void entryModified(CacheEntryModifiedEvent<EmanonKey, EmployeeListEntry> e){
			if (! e.isPre()) Debug.line(e.getValue()) ;
		}
	}
}

class EmployeeListEntry extends AbstractEntry<EmployeeListEntry> {

	private static final long serialVersionUID = 7620380310700586500L;
	private EmanonKey deptId ;
	private List<Employee> emps = ListUtil.newList();
	private EmployeeListEntry(String deptId){
		this.deptId = EmanonKey.create(deptId) ;
	}
	
	public EmployeeListEntry addEmployee(Employee e){
		this.emps.add(e) ;
		return this ;
	}

	public String toString(){
		return emps.toString() ;
	}
	
	@Override
	public EntryKey key() {
		return deptId;
	}
	
}
