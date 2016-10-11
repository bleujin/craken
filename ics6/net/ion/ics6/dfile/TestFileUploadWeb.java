package net.ion.ics6.dfile;

import java.io.File;

import junit.framework.TestCase;
import net.ion.craken.node.crud.Craken;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.radon.aclient.NewClient;
import net.ion.radon.aclient.RequestBuilder;
import net.ion.radon.aclient.Response;
import net.ion.radon.aclient.multipart.FilePart;
import net.ion.radon.aclient.multipart.StringPart;

import org.jboss.netty.handler.codec.http.HttpMethod;

public class TestFileUploadWeb extends TestCase{

	private FileServer fserver;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		System.setProperty("java.net.preferIPv4Stack", "true") ;
		this.fserver = new FileServer().runner(9000); 
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.fserver.shutdown(); 
		super.tearDown();
	}
	
	public void testSingle() throws Exception {
		new InfinityThread().startNJoin(); 
	}
	
	public void testParseInfo() throws Exception {
		RequestBuilder builder = new RequestBuilder().setUrl("http://localhost:9000/web/file/upload").setMethod(HttpMethod.POST);
		builder.addBodyPart(new StringPart("name", "value", "UTF-8"))
			.addBodyPart(new FilePart("myfile", new File("resource/helloworld.txt"), "plain/txt", "UTF-8")) ;

		NewClient client = NewClient.create();
		Response response = client.prepareRequest(builder.build()).execute().get();
		assertEquals(200, response.getStatus().getCode());
		
		Craken craken = fserver.fcontext().craken() ;
		
	}
}
