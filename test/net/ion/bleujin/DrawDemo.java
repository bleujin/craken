package net.ion.bleujin;

import net.ion.framework.util.InfinityThread;

import org.infinispan.util.InfinispanCollections;

import junit.framework.TestCase;

public class DrawDemo extends TestCase {

	public void testRunInstance() throws Exception {
		org.jgroups.demos.Draw.main(new String[0]) ;
		new InfinityThread().startNJoin() ;
	}
}
