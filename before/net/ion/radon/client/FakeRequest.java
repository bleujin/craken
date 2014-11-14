package net.ion.radon.client;

import java.net.URLEncoder;

import net.ion.framework.util.StringUtil;
import net.ion.nradon.HttpHandler;
import net.ion.nradon.stub.StubHttpControl;
import net.ion.nradon.stub.StubHttpRequest;
import net.ion.nradon.stub.StubHttpResponse;
import net.ion.radon.core.TreeContext;

import org.apache.http.client.utils.URLEncodedUtils;
import org.jboss.netty.handler.codec.http.HttpMethod;

public class FakeRequest {

	private HttpHandler handler;
	private StubHttpRequest request;
	
	public FakeRequest(HttpHandler handler, StubHttpRequest request) {
		this.handler = handler ;
		this.request = request ;
	}
	
	public StubHttpResponse get() throws Exception {
		return handle(HttpMethod.GET);
	}

	public StubHttpResponse post() throws Exception {
		return handle(HttpMethod.POST);
	}

	public StubHttpResponse delete() throws Exception {
		return handle(HttpMethod.DELETE);
	}

	public StubHttpResponse put() throws Exception {
		return handle(HttpMethod.PUT);
	}


	public FakeRequest header(String name, String value){
		request.header(name, value) ;
		return this ;
	}
	
	public FakeRequest body(String body){
		request.body(body) ;
		return this ;
	}
	
	
	public FakeRequest postParam(String name, String value){
		String encoded = URLEncoder.encode(value) ;
		String bodyString = request.body() ;
		if (StringUtil.isBlank(bodyString)) body(name + "=" + encoded) ; 
		else body(bodyString + "&" + name + "=" + value) ; 
		return this ;
	}
	
	public StubHttpResponse handle(HttpMethod method) throws Exception {
		TreeContext treeContext = (TreeContext) request.data(TreeContext.class.getCanonicalName()) ;
		treeContext.getParentContext() ;
		request.method(method) ;
		
		StubHttpResponse response = new StubHttpResponse();
		handler.handleHttpRequest(request, response, new StubHttpControl(request, response));
		
		return response;
	}

}
