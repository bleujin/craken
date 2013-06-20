package net.ion.craken.tree;


import javax.transaction.TransactionManager;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.RandomUtil;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;


public class TestPathFirst extends TestCase  {

	private TreeCache tree;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Configuration config = new ConfigurationBuilder().invocationBatching().enable().build() ; // not indexable : indexing().enable().
		final DefaultCacheManager dm = new DefaultCacheManager(config);
		dm.start() ;
		this.tree = new TreeCacheFactory().createTreeCache(dm, "ptest") ;
	}
	
	public void testCreateTreeCache() throws Exception {
		tree.put(Fqn.fromString("persons/bleujin"), MapUtil.chainKeyMap().put("name", "bleujin").put("age", 20).toMap()) ;
		
		Fqn fqn = Fqn.fromString("persons/bleujin");
		TreeNode node = tree.getNode(fqn);
		
		Debug.line(node, node.get("name"), node.getKeys()) ;
		
		TreeNode parent = node.getParent();
		Debug.line(parent) ;
	}

	public void testFindInMany2() throws Exception {
		TransactionManager tm = tree.getCache().getAdvancedCache().getTransactionManager();
		for (int i : ListUtil.rangeNum(200000)) {
			tm.begin() ;
			tree.getCache().put(Fqn.fromString("persons/" + i), MapUtil.chainKeyMap().put("name", "bleujin").put("age", 20).put("num", i).toMap()) ;
			tm.commit() ;
		}
	}
	
	public void testFindInMany() throws Exception {
		
		for (int i : ListUtil.rangeNum(200000)) {
			tree.put(Fqn.fromString("persons/" + RandomUtil.nextInt(10) + "/" + i), MapUtil.chainKeyMap().put("name", "bleujin").put("age", 20).put("num", i).toMap()) ;
		}
		
//		Node node = tree.getNode("persons/555") ;
//		Debug.line(node) ;
	}
	
	
	
}
