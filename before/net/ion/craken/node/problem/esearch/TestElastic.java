package net.ion.craken.node.problem.esearch;

import junit.framework.TestCase;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.radon.aclient.ListenableFuture;
import net.ion.radon.aclient.NewClient;
import net.ion.radon.aclient.Response;

public class TestElastic extends TestCase {

	private NewClient nc;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.nc = NewClient.create();
	}
	
	@Override
	protected void tearDown() throws Exception {
		nc.close() ;
		super.tearDown();
	}
	
	public void testrPut() throws Exception {
		String body = new JsonObject().put("user", "bleujin")
			.put("message", "this is first tweet, lets hope it gets indexed by esearch")
			.put("postDate", "20100201T01:00:00").toString();
		
		ListenableFuture<Response> future = nc.preparePut("http://61.250.201.157:9200/twitter/tweet/1").setBody(body).execute();
		Response response = future.get();
		
		Debug.line(response.getTextBody()) ;
	}

	public void testrPut2() throws Exception {
		String body = new JsonObject().put("user", "bleujin")
			.put("message", "you know, for search")
			.put("postDate", "20100201T01:00:00").toString();
		
		ListenableFuture<Response> future = nc.preparePut("http://61.250.201.157:9200/twitter/tweet/2").setBody(body).execute();
		Response response = future.get();
		
		Debug.line(response.getTextBody()) ;
	}
	

	public void testGet() throws Exception {
		ListenableFuture<Response> future = nc.prepareGet("http://61.250.201.157:9200/twitter/tweet/2").execute();
		Response response = future.get();
		
		Debug.line(response.getTextBody()) ;
		
	}
}
