package net.ion.bleujin;

import junit.framework.TestCase;
import net.ion.framework.util.InfinityThread;

import org.jgroups.tests.McastReceiverTest;
import org.jgroups.tests.McastSenderTest;

public class TestJGroup extends TestCase {

	public void testReceiver() throws Exception {
		McastReceiverTest.main(new String[]{"-mcast_addr", "224.10.10.10", "-port", "5555"});
		
		new InfinityThread().startNJoin() ;
	}
	
	public void testSend() throws Exception {
		McastSenderTest.main(new String[]{"-mcast_addr", "224.10.10.10", "-port", "5555"});
		
		new InfinityThread().startNJoin() ;
	}
	
	
	
}