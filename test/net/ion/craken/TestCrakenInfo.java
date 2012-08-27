package net.ion.craken;

import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import junit.framework.TestCase;

public class TestCrakenInfo extends TestBase {

	public void testAddress() throws Exception {
		assertEquals("my-cluster", craken.getInfo().clusterName()) ;
		Debug.debug(craken.getInfo().memberNames()) ;
		
		craken.stop() ;
		Debug.debug(craken.getInfo().memberNames()) ;
	}
}
