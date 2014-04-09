package net.ion.craken.db;

import java.io.File;
import java.util.concurrent.Executors;

import net.ion.craken.script.FileAlterationMonitor;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import junit.framework.TestCase;

public class TestDetect extends TestCase {

	
	public void testObserver() throws Exception {
		File dir = new File("./test/net/ion/bleujin/script") ;
		FileAlterationObserver observer = new FileAlterationObserver(dir, FileFilterUtils.suffixFileFilter(".js")) ;
		observer.addListener(new FileAlterationListenerAdaptor() {
			@Override
			public void onFileDelete(File file) {
				Debug.line("onDelete", file);
			}
			
			@Override
			public void onFileCreate(File file) {
				Debug.line("onCreate", file);
			}
			
			@Override
			public void onFileChange(File file) {
				Debug.line("onFileChange", file);
			}
		});
		observer.initialize(); 

		FileAlterationMonitor monitor = new FileAlterationMonitor(1000, Executors.newScheduledThreadPool(1), observer) ;
		monitor.start(); 
		
		new InfinityThread().startNJoin(); 
	}
}
