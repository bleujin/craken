package net.ion.ics6.dfile;

import java.util.concurrent.ExecutionException;

import net.ion.framework.dio.FSDataInputStream;
import net.ion.framework.dio.FSError;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import junit.framework.TestCase;

public class TestSingle extends TestCase {
	
	public void testRun() throws Exception {
		System.setProperty("java.net.preferIPv4Stack", "true") ;
		final FileServer fserver = new FileServer().runner(9200);
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				try {
					fserver.shutdown();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		});
		
		Debug.line("started");
		new InfinityThread().startNJoin(); 
	}

}
