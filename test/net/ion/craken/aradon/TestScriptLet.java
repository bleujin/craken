package net.ion.craken.aradon;

import org.restlet.data.Method;

import junit.framework.TestCase;
import net.ion.craken.aradon.bean.RepositoryEntry;
import net.ion.craken.aradon.bean.RhinoEntry;
import net.ion.craken.aradon.let.ResourceFileHandler;
import net.ion.craken.aradon.let.ScriptLet;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.nradon.Radon;
import net.ion.nradon.config.RadonConfiguration;
import net.ion.nradon.handler.aradon.AradonHandler;
import net.ion.radon.aclient.NewClient;
import net.ion.radon.aclient.Request;
import net.ion.radon.aclient.RequestBuilder;
import net.ion.radon.aclient.Response;
import net.ion.radon.core.Aradon;
import net.ion.radon.core.EnumClass.IMatchMode;
import net.ion.radon.core.config.Configuration;
import net.ion.radon.core.config.ConfigurationBuilder;
import net.ion.script.rhino.RhinoEngine;

public class TestScriptLet extends TestCase {
	
	public void testHttpScript() throws Exception {
		RhinoEntry rengine = RhinoEntry.test() ;
		RepositoryEntry repository = RepositoryEntry.test();

		Configuration config = ConfigurationBuilder.newBuilder().aradon()
			.sections().restSection("aradon")
				.addAttribute("repository", repository)
				.addAttribute("rengine", rengine)
				.path("jscript").addUrlPattern("/jscript/{name}.{format}").matchMode(IMatchMode.STARTWITH).handler(ScriptLet.class)
			.build();
		
		Aradon aradon = Aradon.create(config);
		AradonHandler ahandler = AradonHandler.create(aradon);

		
		Radon radon = RadonConfiguration.newBuilder(9000)
			.add("/aradon/*", ahandler)
			.add("/resource/*", new ResourceFileHandler("./resource/"))
			.createRadon() ;
		
		radon.start().get() ;


		NewClient client = NewClient.create();
		
		String script = "session.tranSync(function(wsession){" +
				"	wsession.pathBy('/bleujin').property('name', params.asString('name')).property('age', params.asInt('age'));" +
				"}) ;" +
				"" +
				"session.pathBy('/bleujin').toRows('name, age').toString();" ;
		Request request = new RequestBuilder()
			.setMethod(Method.POST)
			.setUrl("http://61.250.201.157:9000/aradon/jscript/bleujin.string")
				.addParameter("name", "bleujin").addParameter("age", "20")
				.addParameter("script", script).build();

		Response response = client.executeRequest(request).get();
		
		Debug.line(response.getTextBody()) ;
		client.close() ;
		
		radon.stop().get() ;
	}
	
	
	
	
	
}
