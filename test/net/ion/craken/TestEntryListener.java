package net.ion.craken;

import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;

public class TestEntryListener extends TestBase {

	public void xtestFirstServer() throws Exception {

		LegContainer<Employee> emps = craken.defineLeg(Employee.class);
		EntryListener elistener = new EntryListener();
		emps.addListener(elistener);

		new InfinityThread().startNJoin();
	}

	public void xtestSecondServer() throws Exception {
		LegContainer<Employee> emps = craken.defineLeg(Employee.class);
		// EntryListener elistener = new EntryListener();
		// emps.addListener(elistener) ;

		emps.newInstance(7756).name("bleujin").age(20).save();
		emps.findByKey(7756).age(30).save();

		assertEquals(30, emps.findByKey(7756).age());

		new InfinityThread().startNJoin();
	}

	@Listener
	public class EntryListener {

		@CacheEntryCreated
		public void entryCreated(CacheEntryCreatedEvent<EntryKey, Employee> e) {
		}

		@CacheEntryModified
		public void entryModified(CacheEntryModifiedEvent<EntryKey, Employee> e) {
			if (!e.isPre())
				Debug.line(e.getValue(), "Modified");
		}

		@CacheEntryRemoved
		public void entryRemoved(CacheEntryRemovedEvent<EntryKey, Employee> e) {
			Debug.line(e.getValue(), "Removed");
		}
	}

	public void testRecursiveModify() throws Exception {
		LegContainer<Employee> emps = craken.defineLeg(Employee.class);
		emps.addListener(new RecursiveListenr());
		emps.newInstance(7756).name("bleujin").age(20).save();

		new InfinityThread().startNJoin();
	}

	@Listener
	public class RecursiveListenr {
		@CacheEntryModified
		public void entryModified(CacheEntryModifiedEvent<EntryKey, Employee> e) {
			if (e.isPre())
				return;

			Debug.line(e.getValue(), "Modified");
			if (e.getValue().age() < 50) {
				e.getValue().age(e.getValue().age() + 1);
				e.getCache().put(e.getKey(), e.getValue());
			}
		}

	}

}
