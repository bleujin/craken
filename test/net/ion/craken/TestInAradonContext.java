package net.ion.craken;

import java.util.List;

import org.infinispan.util.InfinispanCollections;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;

import net.ion.craken.aradon.CrakenEntry;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.radon.client.AradonClient;
import net.ion.radon.client.AradonClientFactory;
import net.ion.radon.core.Aradon;
import net.ion.radon.core.config.InstanceAttributeValue;
import net.ion.radon.core.config.PathConfiguration;
import net.ion.radon.core.config.SectionConfiguration;
import net.ion.radon.core.let.AbstractServerResource;
import junit.framework.TestCase;

public class TestInAradonContext extends TestCase {

	// start at first compouter
	public void testStart1() throws Exception {
		Aradon aradon = Aradon.create() ;
		aradon.attach(SectionConfiguration.createBlank("")).attach(PathConfiguration.create("share", "/share", ShareLet.class)) ;
		aradon.getServiceContext().putAttribute(CrakenEntry.class.getCanonicalName(), InstanceAttributeValue.create(CrakenEntry.test())) ;
		aradon.start() ;
		
		AradonClient ac = AradonClientFactory.create(aradon) ;
		ac.createRequest("/share").addParameter("empno", "1000").addParameter("name", "bleujin").addParameter("age", "20").post() ;
		
		String text = ac.createRequest("/share").get().getText() ;
		JsonObject jso = JsonParser.fromString(text).getAsJsonArray().get(0).getAsJsonObject() ;
		
		assertEquals("bleujin", jso.asString("name")) ;
		new InfinityThread().startNJoin() ;
	}
	
	// start at another computer
	public void testStart2() throws Exception {
		Aradon aradon = Aradon.create() ;
		aradon.attach(SectionConfiguration.createBlank("")).attach(PathConfiguration.create("share", "/share", ShareLet.class)) ;
		aradon.getServiceContext().putAttribute(CrakenEntry.class.getCanonicalName(), InstanceAttributeValue.create(CrakenEntry.test())) ;
		aradon.start() ;
		
		AradonClient ac = AradonClientFactory.create(aradon) ;
		String text = ac.createRequest("/share").get().getText() ;
		JsonObject jso = JsonParser.fromString(text).getAsJsonArray().get(0).getAsJsonObject() ;
		
		assertEquals("bleujin", jso.asString("name")) ;
	}
	
	
	
}


class ShareLet extends AbstractServerResource {
	
	@Get
	public String viewEmployee(){
		CrakenEntry entry = getContext().getAttributeObject(CrakenEntry.class.getCanonicalName(), CrakenEntry.class) ;
		LegContainer<Employee> leg = entry.getCraken().defineLeg(Employee.class);
		
		return JsonParser.fromObject(leg.findAll()).getAsJsonArray().toString() ;
	}
	
	@Post
	public String addEmployee() throws Exception{
		
		Debug.line(getContext().getAttributes()) ;
		
		CrakenEntry entry = getContext().getAttributeObject(CrakenEntry.class.getCanonicalName(), CrakenEntry.class) ;
		LegContainer<Employee> leg = entry.getCraken().defineLeg(Employee.class);

		int empno = getInnerRequest().getParameterAsInteger("empno") ;
		Employee emp = leg.newInstance(empno).name(getInnerRequest().getParameter("name")).age(getInnerRequest().getParameterAsInteger("age")).save() ;
		
		return JsonParser.fromObject(emp).toString() ;
	} 
	
}