package net.ion.craken.node.problem;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.craken.AbstractEntry;
import net.ion.craken.Craken;
import net.ion.craken.EntryKey;
import net.ion.craken.LegContainer;
import net.ion.craken.simple.EmanonKey;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;
import net.ion.nradon.Radon;
import net.ion.radon.core.Aradon;
import net.ion.radon.impl.let.HelloWorldLet;
import net.ion.radon.util.AradonTester;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;

public class TestMemory extends TestCase {
	private Radon radon;
	private Aradon aradon;
	private Craken craken;

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.create() ;
		craken.globalConfig().transport().clusterName("mycl").addProperty("configurationFile", "./resource/config/jgroups-udp.xml") ;
		
		ExecutorService exec = Executors.newCachedThreadPool();
		this.aradon = AradonTester.create()
				.putAttribute(Craken.class.getCanonicalName(), craken) 
				.putAttribute(ExecutorService.class.getCanonicalName(), exec)
				.register("", "/hello", HelloWorldLet.class)
				.getAradon() ;
		
		
		this.radon = aradon.toRadon(9000);
	}
	
	@Override
	protected void tearDown() throws Exception {
		radon.stop().get() ;
		super.tearDown();
	}
	
	
	public void testMemory() throws Exception {
		craken.preDefineConfig(MyEntry.class, new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_ASYNC).eviction().clustering().l1().hash().numOwners(1).build()) ;
		craken.start() ;
		
		ExecutorService exec = Executors.newCachedThreadPool();
		for (int cc : ListUtil.rangeNum(20)) {
			exec.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					LegContainer<MyEntry> container = craken.defineLeg(MyEntry.class);
					for (int i = 0, last = 1000000 ; i < last ; i++) {
						container.mergeInstance(String.valueOf(RandomUtil.nextInt(1000000))).save() ;
						Thread.sleep(10) ;
						if ((i % 1000) == 0) System.out.println(i) ;
					}
					return null;
				}
			}) ;
		}

		radon.start().get() ;
		
		new InfinityThread().startNJoin() ;
	}
	
	public void testPacketLength() throws Exception {
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ObjectOutputStream oout = new ObjectOutputStream(bout);
		
		oout.writeObject(new MyEntry("key")) ;
		
		byte[] bytes = bout.toByteArray();
		Debug.line(bytes.length) ;
	}
	

}

class MyEntry extends AbstractEntry<MyEntry> {

//	private NormalMessagePacket packet ;
	private String packet ;
	
	private EmanonKey key ;
	public MyEntry(String id) throws FileNotFoundException, IOException{
		this.key = EmanonKey.create(id) ;
//		this.packet = NormalMessagePacket.load(IOUtil.toString(new FileInputStream("./test/net/ion/craken/problem/spacket.txt"))) ;
		final FileInputStream input = new FileInputStream("./test/net/ion/craken/problem/spacket.txt");
		this.packet = IOUtil.toString(input) ;
		IOUtil.closeQuietly(input) ;
	}
	
	@Override
	public EntryKey key() {
		return key;
	}
	
	public Object packet(){
		return packet ;
	} 
	
	
	
}
