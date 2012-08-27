package net.ion.craken;

import java.util.Properties;

import net.ion.framework.util.Debug;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.FileCacheStoreConfigurationBuilder.FsyncMode;

public class TestWriteFile extends TestBase{

	
	public void testFileWrite() throws Exception {
		Configuration config = new ConfigurationBuilder().loaders().addFileCacheStore()
			.ignoreModifications(false).fetchPersistentState(true).purgeOnStartup(true).location("./resource/temp")
			.async().enabled(false).build();

		LegContainer<Employee> emps = craken.defineLeg(Employee.class, config);
		
		for (int i = 0; i < 10; i++) {
			emps.newInstance(i).name(i + "'s").save() ;
		}
	}
	
	public void testFileRead() throws Exception {
		Configuration config = new ConfigurationBuilder().loaders().preload(true).addFileCacheStore()
			.ignoreModifications(false).fetchPersistentState(true).purgeOnStartup(false).location("./resource/temp").purgeSynchronously(true)
			.async().enabled(true).build();

		LegContainer<Employee> emps = craken.defineLeg(Employee.class, config);
		assertEquals(10, emps.keySet().size()) ;
	}
}
