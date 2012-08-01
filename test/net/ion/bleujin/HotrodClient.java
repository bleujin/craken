package net.ion.bleujin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;

public class HotrodClient {

	public static void main(String[] args) throws IOException {

		int records = 100;
		String cacheName = "test-dist-cache";
		Properties properties = new Properties();
		properties.setProperty("testOnBorrow", "true");
		properties.setProperty("testWhileIdle", "true");
		properties.setProperty(ConfigurationProperties.SERVER_LIST, "127.0.0.1:11222;127.0.0.1:11223");

		System.out.println("Starting the Hotrod Client\n");

		RemoteCacheManager remoteCacheManager = new RemoteCacheManager(properties);
		RemoteCache<String, String> remoteCache = remoteCacheManager.getCache();

		for (int i = 0; i < records; i++) {
			remoteCache.put("key" + i, "value" + i);
		}

		System.out.println("Loaded " + records + " records into the EDG cache\n");

		BufferedReader bs = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("Press any key to check the records in the cache or 'X' to exit");
		while (!(bs.readLine().equalsIgnoreCase("X"))) {
			System.out.println("Checking to see how many of the " + records + " records can be found in the cache");
			int found = 0;
			for (int i = 0; i < records; i++) {
				if (remoteCache.get("key" + i) != null) {
					found++;
				}
			}
			System.out.println("Found " + found + " of " + records + " records.");
		}

		remoteCacheManager.stop();
		System.out.println("Exiting");

	}

}
