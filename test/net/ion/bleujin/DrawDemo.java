package net.ion.bleujin;

import junit.framework.TestCase;
import net.ion.framework.util.InfinityThread;

public class DrawDemo extends TestCase {

	public void testRunInstance() throws Exception {
		org.jgroups.demos.Draw.main(new String[0]) ;
		new InfinityThread().startNJoin() ;
	}
}
