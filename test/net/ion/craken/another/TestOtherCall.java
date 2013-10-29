package net.ion.craken.another;

import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.TreeNodeKey;

import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;

import junit.framework.TestCase;

public class TestOtherCall extends TestCase{

	private DefaultCacheManager dfm;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.dfm = new DefaultCacheManager();
		dfm.start() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		dfm.stop() ;
		super.tearDown();
	}
	
	
	public void testCreate() throws Exception {
		Cache<TreeNodeKey, Object> cache = dfm.getCache("test");
		
		cache.put(Fqn.fromString("/bleujin").contentKey(), "/bleujin") ;
	}
}
