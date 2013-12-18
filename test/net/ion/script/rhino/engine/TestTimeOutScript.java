package net.ion.script.rhino.engine;

import net.ion.framework.util.InfinityThread;
import net.ion.script.rhino.TestBaseScript;

public class TestTimeOutScript extends TestBaseScript{

	
	public void xtestTimeOut() throws Exception {
		final String infinityScript = "" +
				"var i = 0 ;" +
				"while(true){" +
				"	java.lang.Thread.sleep(1000);" +
				"	print(i++)" +
				"}";
		
		rengine.newScript("infinity").defineScript(infinityScript).exec() ;
	}
	
	public void xtestTimeOutOnMulti() throws Exception {
		final String infinityScript = "" +
				"var i = 0 ;" +
				"while(true){" +
				"	java.lang.Thread.sleep(1000);" +
				"	print(i++)" +
				"}";
		
		for (int i = 0; i < 3; i++) {
			new Thread(){
				public void run(){
					rengine.newScript("infinity").defineScript(infinityScript).exec() ;
				}
			}.start() ;
		}
		
		new InfinityThread().startNJoin() ;
	}
}
