package net.ion.craken.node.problem;

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
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.RandomUtil;
import net.ion.nradon.Radon;
import net.ion.nradon.let.IServiceLet;
import net.ion.radon.client.AradonClient;
import net.ion.radon.client.AradonClientFactory;
import net.ion.radon.core.Aradon;
import net.ion.radon.core.TreeContext;
import net.ion.radon.core.annotation.AnContext;
import net.ion.radon.impl.let.HelloWorldLet;
import net.ion.radon.util.AradonTester;

import org.restlet.Response;
import org.restlet.data.Method;
import org.restlet.resource.Get;


public class TestUDP extends TestCase {

	private Radon radon;
	private Aradon aradon;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Craken craken = Craken.create() ;
		craken.globalConfig().transport().clusterName("mycl").addProperty("configurationFile", "./resource/config/jgroups-udp.xml") ;
		craken.start() ;
		
		
		ExecutorService exec = Executors.newCachedThreadPool();
		this.aradon = AradonTester.create()
				.putAttribute(Craken.class.getCanonicalName(), craken) 
				.putAttribute(ExecutorService.class.getCanonicalName(), exec)
				.register("", "/hello", HelloWorldLet.class)
				.register("", "/startping", StartRun.class)
				.getAradon() ;
		
		
		this.radon = aradon.toRadon(9000);
	}
	
	@Override
	protected void tearDown() throws Exception {
		radon.stop().get() ;
		super.tearDown();
	}
	
	public void testStartAradon() throws Exception {
		radon.start().get() ;
		
		Debug.line(radon.getConfig().getServiceContext().getAttributes()) ;
		
		AradonClient client = AradonClientFactory.create(aradon) ;
		Response response = client.createRequest("/startping").handle(Method.GET) ;
		
		Debug.line(response.getEntityAsText()) ;
		new InfinityThread().startNJoin() ;
	}
	
	
	
}


class StartRun implements IServiceLet {
	
	@Get
	public String runUDP(@AnContext TreeContext context){
		Craken craken = context.getAttributeObject(Craken.class.getCanonicalName(), Craken.class) ;
		final LegContainer<Employee> container = craken.defineLeg(Employee.class) ;

		ExecutorService exec = context.getAttributeObject(ExecutorService.class.getCanonicalName(), ExecutorService.class) ;
		exec.submit(new Callable<Void>(){
			public Void call() throws Exception {
				int max = 10000 ;
				while(max-- > 0){
					container.mergeInstance(String.valueOf(RandomUtil.nextInt(10000))).age(RandomUtil.nextInt(100)) ;
					Thread.sleep(10) ;
					if ((max % 100) == 0) System.out.print('.') ;
				}
				return null;
			}
		}) ;
		
		return "started" ;
	}
	
	
}


class Employee extends AbstractEntry<Employee>{

	private static final long serialVersionUID = 9065832165725604762L;
	private EmanonKey key ;
	private int age;
	public Employee(String name){
		this.key = EmanonKey.create(name) ;
	}

	public Employee age(int age){
		this.age = age ;
		return this ;
	}
	
	public int age(){
		return age ;
	}
	
	@Override
	public EntryKey key() {
		return key;
	}

	
}
