package net.ion.craken.node;

import java.io.IOException;

import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.schedule.IExecutor;

public interface Repository {

	public void start() ;
	
	public void shutdown()  ;

	public IExecutor executor() ;
	
	public ReadSession testLogin(String wsname) throws IOException  ;
	
	public ReadSession login(Credential credential, String wsname) throws IOException  ;
	
	public <T> T getAttribute(String key, Class<T> clz) ;
	
	public RepositoryImpl putAttribute(String key, Object value) ;
}
