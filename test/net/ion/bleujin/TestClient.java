package net.ion.bleujin;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.RandomUtil;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.client.hotrod.impl.transport.tcp.RoundRobinBalancingStrategy;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.server.core.Main;

public class TestClient extends TestCase {

	public void testInputSample() throws Exception {
		final DefaultCacheManager dc = new DefaultCacheManager("resource/config/distributed-simple.xml");
		dc.getCache("fileCache").put("date", new Date());
		dc.getCache().put("name", "bleujin");
	}

	public void xtestRunServer() throws Exception {
//		Main.main(new String[] { "-r", "hotrod", "-c", "resource/config/distributed-simple.xml", "-p", "11222" });
		Main.main(new String[] { "-r", "hotrod", "-c", "resource/config/distributed-simple.xml", "-p", "11223"});
		// Main.main(new String[] { "-r", "hotrod", "-c", "resource/config/distributed-simple.xml", "-p", "11224"});
		new InfinityThread().startNJoin();
	}

	public void testDefaultCacheManager() throws Exception {
		final DefaultCacheManager dc = new DefaultCacheManager("resource/config/distributed-simple.xml");
		Debug.line(dc.getCache().get("name"));
		Debug.line(dc.getCache("fileCache").get("date"));
	}

	public void testSetRemoteCacheStore() throws Exception {
		new Thread() {
			public void run() {
				try {
					DefaultCacheManager rcm = new DefaultCacheManager("resource/config/hotrod-client.xml");
					while (true) {
						rcm.getCache().put("name", RandomUtil.nextRandomString(8));
						rcm.getCache("fileCache").put("date", new Date());
						Thread.sleep(1000) ;
					}
				} catch (Throwable ignore) {
					ignore.printStackTrace();
				}
			}
		}.start();
		new InfinityThread().startNJoin();
	}

	public void testRemoteCacheStore() throws Exception {
		DefaultCacheManager rcm = new DefaultCacheManager("resource/config/hotrod-client.xml");
		Cache<Object, Object> defaultCache = rcm.getCache();
		Cache<Object, Object> fileCache = rcm.getCache("fileCache");

		// Debug.line(defaultCache.get("name"));
		// Debug.line(fileCache.get("date"));

		BufferedReader bs = new BufferedReader(new InputStreamReader(System.in));
		while (!(bs.readLine().equalsIgnoreCase("X"))) {
			Debug.line(defaultCache.get("name"));
			Debug.line(fileCache.get("date"));
		}

		rcm.stop();
	}
}

class CalendarCache extends Thread {

	private CacheContainer cm;

	protected CalendarCache(CacheContainer cm) {
		super(CalendarCache.class.getCanonicalName());
		this.cm = cm;
	}

	public void run() {
		try {
			Cache<Object, Object> cache = cm.getCache("fileCache");
			cache.put("calendar", new Date());
			while (true) {
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
