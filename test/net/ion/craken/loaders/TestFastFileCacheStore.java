package net.ion.craken.loaders;

import java.io.Serializable;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestFastFileCacheStore extends TestCase {

	private GlobalConfiguration globalConf;
	private DefaultCacheManager dftManager;
	private Configuration defaultConf ;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		globalConf = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("infinispan-test-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml").build();
		this.defaultConf = createFastLocalCacheStore() ;
		dftManager = new DefaultCacheManager(globalConf, defaultConf, false);
		dftManager.start() ;
	}

	@Override
	protected void tearDown() throws Exception {
		dftManager.stop();
		super.tearDown();
	}
	
	public void testPut() throws Exception {
		Cache<String, Employee> cache = dftManager.getCache("employee");
//		cache.clear() ;
		for (int i = 0; i < 10; i++) {
			cache.put("test" + i, Employee.createEmp(20, "test" + i, 7789 + i));
		}

		Debug.line(cache.keySet().size()) ;
	}

	private static org.infinispan.configuration.cache.Configuration createFastLocalCacheStore() {
		return new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).clustering().l1().enable().invocationBatching().clustering().hash().numOwners(2).unsafe()
		// .eviction().maxEntries(1000)
				.invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty("location", "./resource/temp")
				// ./resource/temp
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build();
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
