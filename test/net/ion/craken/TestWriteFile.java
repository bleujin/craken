package net.ion.craken;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;

public class TestWriteFile extends TestBase{

	
	public void testWriteHundred() throws Exception {
		Configuration config = new ConfigurationBuilder().loaders().addFileCacheStore()
			.ignoreModifications(false).fetchPersistentState(true).purgeOnStartup(true).location("./resource/temp")
			.async().enabled(false)
			.eviction().maxEntries(10)
			.build();

		LegContainer<Employee> emps = craken.defineLeg(Employee.class, config);
		
		for (int i = 0; i < 100; i++) {
			emps.newInstance(i).name(i + "'s").save() ;
		}
		
		
	}

	
	public void testRead() throws Exception {
		Configuration config = new ConfigurationBuilder().loaders().preload(true).addFileCacheStore()
			.ignoreModifications(false).fetchPersistentState(true).purgeOnStartup(false).location("./resource/temp").purgeSynchronously(true)
			.async().enabled(true).build();

		LegContainer<Employee> emps = craken.defineLeg(Employee.class, config);
		assertEquals(10, emps.keySet().size()) ;
	}
	
	public void testFileDelete() throws Exception {
		Configuration config = new ConfigurationBuilder().loaders().preload(true).addFileCacheStore()
		.ignoreModifications(false).fetchPersistentState(true).purgeOnStartup(false).location("./resource/temp").purgeSynchronously(true)
		.async().enabled(true).build();

		LegContainer<Employee> emps = craken.defineLeg(Employee.class, config);
		emps.findByKey(1).remove() ;
	}
}
