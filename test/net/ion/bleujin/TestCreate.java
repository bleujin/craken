package net.ion.bleujin;

import java.util.List;

import junit.framework.TestCase;
import net.ion.craken.AbstractEntry;
import net.ion.craken.Craken;
import net.ion.craken.LegContainer;
import net.ion.craken.EntryFilter;
import net.ion.craken.EntryKey;
import net.ion.craken.simple.SimpleKeyFactory;
import net.ion.craken.simple.SimpleEntry;
import net.ion.framework.db.Page;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;

public class TestCreate extends TestCase{

	private Craken craken ;
	
	protected void setUp() throws Exception {
		this.craken = Craken.create();
	}
	
	protected void tearDown() throws Exception {
		craken.stop() ;
	}
	
	public void testCreate() throws Exception {
		craken.globalConfig().transport().clusterName("my-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml") ;
		craken.start() ;
		LegContainer<SimpleEntry> container = craken.defineLeg(SimpleEntry.class,  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).jmxStatistics().enable().clustering().invocationBatching().build()) ;
		craken.addListener(new ContainerListener()) ;
		

		// container.addListener(new EntryListener()) ;

		while(true){
			SimpleEntry node = container.newInstance("bleujin" + RandomUtil.nextInt(10)).put("age", RandomUtil.nextInt(100)).put("server", craken.getManager().getAddress().toString());
			node.save() ;
			Thread.sleep(1000) ;
		}
	}
	
	
	public void testEmpNode() throws Exception {
		LegContainer<EmpEntry> container = craken.defineLeg(EmpEntry.class,  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).jmxStatistics().enable().clustering().invocationBatching().build()) ;
//		container.addListener(new EntryListener()) ;
		
		EmpEntry emp = container.newInstance("7789") ;
		emp.name("bleujin").age(20) ;
		emp.save() ;
		
		assertEquals("bleujin", container.findByKey("7789").name()) ;
	}
	
	
	public void testConfirmEmp() throws Exception {
		LegContainer<EmpEntry> container = craken.defineLeg(EmpEntry.class) ;
		
		for (EntryKey key : container.keySet()){
			Debug.line(container.findByKey(key)) ;
		}
	}
	
	
	public void testRET() throws Exception {
		LegContainer<SimpleEntry> container = craken.defineLeg(SimpleEntry.class) ;
		
		container.newInstance("bleujin").put("name", "bleujin").put("age", 20).save() ;
		container.newInstance(7756).put("name", "hero").put("age", 364).save() ;
		
		
		SimpleEntry bleujin = container.findByKey("bleujin") ;
		Debug.line(bleujin.field("age"), bleujin.fieldAsInt("age") + 1, bleujin.fieldAsString("name")) ;

		SimpleEntry f7756 = container.findByKey(7756) ;
		Debug.line(f7756.field("age"), f7756.fieldAsInt("age") + 1, f7756.fieldAsString("name")) ;

	}
	
	
	public void testFindOne() throws Exception {
		LegContainer<SimpleEntry> container = craken.defineLeg(SimpleEntry.class) ;
		
		container.newInstance("bleujin").put("name", "bleujin").put("age", 20).save() ;
		container.newInstance(7756).put("name", "hero").put("age", 364).save() ;

		SimpleEntry found = container.findOne(new EntryFilter<SimpleEntry>(){
			public boolean filter(SimpleEntry node){
				return node.fieldAsInt("age") > 100;
			}
		}) ;
		
		assertEquals(364, found.fieldAsInt("age")) ;
	}
	
	
	public void testFind() throws Exception {
		LegContainer<SimpleEntry> container = craken.defineLeg(SimpleEntry.class) ;
		
		for (int i = 0; i < 15 ; i++) {
			container.newInstance("user" + i).put("index", i).save() ;
		}

		List<SimpleEntry> found = container.find(new EntryFilter<SimpleEntry>(){
			public boolean filter(SimpleEntry node){
				return node.fieldAsInt("index") < 5;
			}
		}) ;
		
		assertEquals(5, found.size()) ;

		List<SimpleEntry> foundPage = container.find(new EntryFilter<SimpleEntry>(){
			public boolean filter(SimpleEntry node){
				return node.fieldAsInt("index") < 5;
			}
		}, Page.create(2, 2)) ;
		
		assertEquals(2, foundPage.size()) ;
	}

	
	
	
	
	
	
	@Listener
	public class ContainerListener {
		@CacheStarted
		public void cacheStarted(CacheStartedEvent e){
			Debug.line('c', "") ;
		} 
		
		@CacheStopped
		public void cacheStopped(CacheStoppedEvent e){
		}
		
		@ViewChanged
		public void viewChanged(ViewChangedEvent e){
			Debug.line('x', e.getOldMembers(), e.getNewMembers()) ;
		}
	}
	
	

	@Listener
	public class EntryListener {

		public EntryListener() {
		}

		@CacheEntryCreated
		public void cacheEntryCreated(CacheEntryCreatedEvent<EntryKey, AbstractEntry> e) {
			// if (!e.isPre()) Debug.line(e.getKey()) ;
		}

		@CacheEntryModified
		public void cacheEntryModified(CacheEntryModifiedEvent<EntryKey, AbstractEntry> e) {
			if (!e.isPre()) Debug.line(e.getKey(), e.getValue()) ;
		}
	}
	
	
}


class EmpEntry extends AbstractEntry {

	private static final long serialVersionUID = 5166614064200120569L;
	private final String empNo ;
	
	private int age;
	private String name;
	
	private EmpEntry(String empNo){
		this.empNo = empNo ;
	}
	
	@Override
	public EntryKey key() {
		return SimpleKeyFactory.create(empNo);
	}

	public EmpEntry name(String name) {
		this.name = name ;
		return this;
	}

	public EmpEntry age(int age) {
		this.age = age ;
		return this;
	}

	public String name() {
		return name ;
	}
	
}

