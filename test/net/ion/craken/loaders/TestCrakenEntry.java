package net.ion.craken.loaders;

import java.io.Serializable;

import junit.framework.TestCase;
import net.ion.craken.AbstractEntry;
import net.ion.craken.Craken;
import net.ion.craken.EntryKey;
import net.ion.craken.LegContainer;
import net.ion.craken.simple.EmanonKey;
import net.ion.framework.util.Debug;
import net.ion.radon.core.PageBean;
import net.ion.radon.repository.RepositoryCentral;
import net.ion.radon.repository.Session;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.loaders.file.FileCacheStore;

public class TestCrakenEntry extends TestCase {

	private Craken craken;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.create() ;
		
		craken.globalConfig().transport().clusterName("infinispan-test-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml");
		craken.start() ;
	}
	
	
	private Configuration createLocalCacheStore(){
		return new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).clustering().l1().enable().invocationBatching()
		.clustering().hash().numOwners(2).unsafe()
		.eviction().maxEntries(1000)
		.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty("location", "./resource/temp") // ./resource/temp
		.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build() ;
	}
	
	private Configuration createFastLocalCacheStore(){
		return new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).clustering().l1().enable().invocationBatching()
		.clustering().hash().numOwners(2).unsafe()
		.eviction().maxEntries(1000)
		.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty("location", "./resource/temp") // ./resource/temp
		.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build() ;
	}
	
	
	public void testSerial() throws Exception {
		RepositoryCentral rc = RepositoryCentral.testCreate();
		Session session = rc.login("serial");
		
		session.newNode().put("key", EmanonKey.create("bleujin")).getSession().commit() ;
		
	}
	
	public void testSave() throws Exception {
		LegContainer<Person> econ = craken.defineLeg(Person.class);
		econ.newInstance("bleujin").address(Address.create("seoul")).age(30).save() ;
		
		Thread.sleep(1000) ;
	}

	
	public void testFind() throws Exception {
		RepositoryCentral rc = RepositoryCentral.testCreate() ;
		rc.login("craken", "mycol").createQuery().aradonGroupId(Person.class.getCanonicalName(), "bleujin").find().debugPrint(PageBean.ALL) ;
	}
	
	public void testLoad() throws Exception {
		
		LegContainer<Person> econ = craken.defineLeg(Person.class);
		Person person = econ.findOne() ;
		assertEquals("bleujin", person.key().get()) ;
		assertEquals("busan", person.address().city()) ;
	}
	
	public void xtestLoop() throws Exception {
		craken.preDefineConfig(Person.class, createFastLocalCacheStore()) ;
		LegContainer<Person> econ = craken.defineLeg(Person.class);
		econ.clear() ;
		long start = System.currentTimeMillis() ;
		for (int i = 0; i < 10000 ; i++) {
			econ.newInstance("" + i).address(Address.create("seoul")).age(30).save() ;
			if (i % 1000 == (1000-1)) Debug.line(i / 1000, System.currentTimeMillis() - start) ;
		}
		econ.newInstance("bleujin").address(Address.create("seoul")).age(30).save() ;
		Thread.sleep(500) ;
	}
	
	public void testUpdate() throws Exception {
		craken.preDefineConfig(Person.class, createFastLocalCacheStore()) ;
		LegContainer<Person> econ = craken.defineLeg(Person.class);

		long start = System.currentTimeMillis() ;
		for (int i = 0; i < 10000 ; i++) {
			econ.newInstance("" + i).address(Address.create("longseoul")).age(30).save() ;
			if (i % 1000 == (1000-1)) Debug.line(i / 1000, System.currentTimeMillis() - start) ;
		}
		econ.newInstance("bleujin").address(Address.create("seoul")).age(30).save() ;
		Thread.sleep(500) ;
	}
	
	public void xtestLoad() throws Exception {
		craken.preDefineConfig(Person.class, createFastLocalCacheStore()) ;
		LegContainer<Person> econ = craken.defineLeg(Person.class);
		assertEquals(30, econ.findByKey("bleujin").age()) ;
		Debug.line(econ.keySet().size()) ;
	}
	
	
	
}

class Person extends AbstractEntry<Person> {

	private static final long serialVersionUID = -3220216562694740835L;
	private EmanonKey key ;
	private int age ;
	private Address address ;
	
	public Person(String name){
		this.key = EmanonKey.create(name) ;
	}
	
	@Override
	public EntryKey key() {
		return key;
	}
	
	public Address address(){
		return address ;
	}
	
	public int age(){
		return age ;
	}

	public Person address(Address address){
		this.address = address ;
		return this ;
	}
	
	public Person age(int age){
		this.age = age ;
		return this ;
	}
	
	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}

}


class Address implements Serializable {
	
	private static final long serialVersionUID = 6442525226894500755L;
	private String city ;
	Address(String city){
		this.city = city ;
	}
	
	static Address create(String city){
		return new Address(city) ;
	}
	
	public String city(){
		return city ;
	}
}



