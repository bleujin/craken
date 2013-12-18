package net.ion.craken.aradon;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import org.restlet.data.Method;
import org.restlet.resource.Post;
import org.restlet.resource.Put;

import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.nradon.Radon;
import net.ion.nradon.let.IServiceLet;
import net.ion.radon.aclient.FluentStringsMap;
import net.ion.radon.aclient.NewClient;
import net.ion.radon.aclient.Request;
import net.ion.radon.aclient.RequestBuilder;
import net.ion.radon.aclient.Response;
import net.ion.radon.aclient.multipart.FilePart;
import net.ion.radon.aclient.multipart.StringPart;
import net.ion.radon.core.Aradon;
import net.ion.radon.core.annotation.AnRequest;
import net.ion.radon.core.let.InnerRequest;
import net.ion.radon.core.let.MultiValueMap;
import net.ion.radon.util.AradonTester;
import junit.framework.TestCase;

public class TestParameterMap extends TestCase {

	private Radon radon;
	private NewClient client;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Aradon aradon = AradonTester.create()
			.register("test", "/param", ParamLet.class)
			.register("test", "/params", ParamsLet.class).getAradon();
		this.radon = aradon.toRadon(8999).start().get();
		this.client = NewClient.create();
	}
	
	@Override
	protected void tearDown() throws Exception {
		radon.stop().get() ;
		client.close();
		super.tearDown();
	}
	
	public void testParamType() throws Exception {
		Response response = client.preparePost("http://61.250.201.157:8999/test/param").addParameter("string", "안녕").addParameter("int", "1").addParameter("long", "1").execute().get();
		assertEquals("ok", response.getTextBody()) ;
	}
	
	public void testMultiPartType() throws Exception {
		
		Response response = client.preparePut("http://61.250.201.157:8999/test/param")
			.addBodyPart(new StringPart("string", "안녕"))
			.addBodyPart(new StringPart("int", "1"))
			.addBodyPart(new StringPart("long", "1"))
			.addBodyPart(new FilePart("file", new File("./resource/testScript.js")))
			.execute().get();
		
		assertEquals("ok", response.getTextBody()) ;		
	}
	
	

	public void xtestParamsType() throws Exception {
//		Response response = client.preparePost("http://61.250.201.157:8999/test/params")
//			.addBodyPart(new StringPart("string", "안녕"))
//			.addBodyPart(new StringPart("string", "안녕1"))
//			.addBodyPart(new StringPart("int", "1")).addBodyPart(new StringPart("int", "2"))
//			.addBodyPart(new StringPart("long", "1")).addBodyPart(new StringPart("long", "2"))
//			.addBodyPart(new FilePart("file", new File("./resource/testScript.js")))
//			.execute().get();
		
		final Request request = new RequestBuilder()	
			.setUrl("http://61.250.201.157:8999/test/params").setMethod(Method.POST)
			.addParameter("string", "안녕").addParameter("string", "안녕2").build();
		Debug.line(request.getParams()) ;

		
		Response response = client.executeRequest(request).get();
		
		
		Debug.line("ok", response.getTextBody()) ;
	}

	
	
	
}

class ParamLet implements IServiceLet {

	@Put
	public String viewStremaParam(@AnRequest InnerRequest req) throws IOException{
		ParameterMap params = ParameterMap.create(req.getFormParameter());
		Debug.line(params.asString("string"), params.asInt("int"), params.asLong("long"), IOUtil.toStringWithClose(params.asStream("file")) ) ;
		
		return "ok" ;
	}
	
	@Post
	public String viewParam(@AnRequest InnerRequest req){
		ParameterMap params = ParameterMap.create(req.getFormParameter());
		Debug.line(params.asString("string"), params.asInt("int"), params.asLong("long")) ;
		
		return "ok" ;
	}
}


class ParamsLet implements IServiceLet {
	@Post
	public String viewParams(@AnRequest InnerRequest req){
		
		ParameterMap params = ParameterMap.create(req.getFormParameter());
		
		
		
		Debug.line(params.asStrings("string")) ;
		
		return "ok" ;
	}
	
	
}


