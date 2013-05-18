package net.ion.craken.loaders;

import java.io.Serializable;
import java.util.List;

import net.ion.craken.loaders.NewMongoDBCacheStore;
import net.ion.framework.db.DBController;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.manager.OracleCacheDBManager;
import net.ion.framework.db.manager.OracleDBManager;
import net.ion.framework.db.manager.OraclePoolDBManager;
import net.ion.framework.db.mongo.jdbc.MongoConnection;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.jboss.marshalling.SerializabilityChecker;

import scala.actors.threadpool.Arrays;

import junit.framework.TestCase;

public class TestMongoDBCacheStore extends TestCase {

	private GlobalConfiguration globalConf;
	private DefaultCacheManager dftManager;
	private Configuration defaultConf ;

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		globalConf = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("craken").addProperty("configurationFile", "resource/config/jgroups-udp.xml").build();
		this.defaultConf = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).clustering().l1().enable().invocationBatching()
			.clustering().hash().numOwners(2).unsafe()
			.eviction().maxEntries(100)
			.loaders().preload(true).shared(true).addCacheLoader().cacheLoader(new NewMongoDBCacheStore()).addProperty("host", "61.250.201.78").addProperty("dbName", "craken").addProperty("dbCollection", "mycol")
			.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(true)
			.build();
		dftManager = new DefaultCacheManager(globalConf, defaultConf, false);
		dftManager.start();
	}

	@Override
	protected void tearDown() throws Exception {
		dftManager.stop();
		super.tearDown();
	}
	
	public void testClear() throws Exception {
		Cache<String, Employee> cache = dftManager.getCache("employee");
		cache.clear() ;
	}
	
	public void testMaxEntry() throws Exception {
		Cache<String, Employee> cache = dftManager.getCache("employee");
		for (int i = 0; i < 1000; i++) {
			cache.put("test" + i, Employee.createEmp(20, "test" + i, 7789 + i));
		}

		Debug.line(cache.keySet().size()) ;
	}

	
	
	public void testSave() throws Exception {
		Cache<String, Employee> cache = dftManager.getCache("employee");
		cache.clear() ;
		
		cache.put("jin", Employee.createEmp(20, "jin", 7789));
		cache.put("hero", Employee.createEmp(22, "hero", 7790));
		cache.put("bleujin", Employee.createEmp(24, "bleujin", 7791));

		Employee hero = cache.get("hero");
		assertEquals(7790, hero.getEmpno());
	}

	public void testGet() throws Exception {
		Cache<String, Employee> cache = dftManager.getCache("employee");
		cache.clear() ;
		
		cache.put("jin", Employee.createEmp(20, "jin", 7789));
		cache.put("hero", Employee.createEmp(22, "hero", 7790));
		cache.put("bleujin", Employee.createEmp(24, "bleujin", 7791));
		cache.stop() ;
		
		cache.start() ;
		
		Employee hero = cache.get("hero");
		Debug.linec('x', hero) ;
	}

	
	public void testDebugPrint() throws Exception {
		// dftManager.defineConfiguration("employee", defaultConf) ;
		Cache<String, Employee> cache = dftManager.getCache("employee");
		cache.compact() ;
		Debug.line(cache.keySet().size(), cache.get("jin"), cache.get("bleujin"), cache.get("hero"), cache.keySet().size()) ;
	}
	
	
	public void testAboutType() throws Exception {
		Cache<Long, String> cache = dftManager.getCache("string") ;
		cache.clear() ;
		
		cache.put(35L, "bleujin") ;
		cache.put(40L, "hero") ;
		
		cache.stop() ;
		cache.start() ;
		
		assertEquals(2, cache.values().size()) ;
	}

	public void testNotSupportedType() throws Exception {
		Cache<Long, List<String>> cache = dftManager.getCache("list") ;
		cache.clear() ;
		
		cache.put(35L, ListUtil.toList("bleujin")) ;
		cache.put(40L, ListUtil.toList("hero", "jin")) ;
		
		cache.stop() ;
		cache.start() ;
		
		assertEquals(0, cache.values().size()) ;
	}

	public void testWrapperObject() throws Exception {
		Cache<Long, Wrapper> cache = dftManager.getCache("wrapper") ;
		cache.clear() ;
		
		cache.put(35L, Wrapper.create("bleujin", ListUtil.toList(Employee.createEmp(20, "bleujin", 7789), Employee.createEmp(21, "hero", 7790)))) ;
		cache.put(40L, Wrapper.create("bleujin", ListUtil.toList(Employee.createEmp(20, "bleujin", 7789), Employee.createEmp(21, "hero", 7790)))) ;
		
		cache.stop() ;
		cache.start() ;
		
		assertEquals(2, cache.values().size()) ;
		Wrapper w = cache.get(35) ;
		assertEquals(2, w.emps().size()) ;
		
	}
}



class Wrapper implements Serializable {
	private static final long serialVersionUID = -3015823701312994982L;
	private String name ;
	private List<Employee> emps = ListUtil.newList() ;
	
	Wrapper(String name, List<Employee> emps){
		this.name = name ;
		this.emps = emps ;
	}
	
	static Wrapper create(String name, List<Employee> emps){
		return new Wrapper(name, emps) ;
	}
	
	public List<Employee> emps(){
		return emps ;
	}
	
}



class Employee implements Serializable {

	private int deptno;
	private String ename;
	private int empno;
	
	private static final long serialVersionUID = 9214867003608719747L;
	private Employee() {
	}

	static Employee createEmp(int deptno, String name, int empno) {
		Employee emp = new Employee();
		emp.setDeptno(deptno);
		emp.setEname(name);
		emp.setEmpno(empno);
		return emp;
	}

	public int getDeptno() {
		return deptno;
	}

	public void setDeptno(int deptno) {
		this.deptno = deptno;
	}

	public String getEname() {
		return ename;
	}

	public void setEname(String ename) {
		this.ename = ename;
	}

	public int getEmpno() {
		return empno;
	}

	public void setEmpno(int empno) {
		this.empno = empno;
	}

	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}
}
