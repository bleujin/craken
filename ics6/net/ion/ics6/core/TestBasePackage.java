package net.ion.ics6.core;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.script.CrakenScript;
import junit.framework.TestCase;

public class TestBasePackage extends TestCase{

	
	private RepositoryImpl r;
	private ScheduledExecutorService ses;

	protected ReadSession session;
	protected CrakenScript cs;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
		r.start() ;
		this.session = r.login("test") ;
		this.ses = Executors.newScheduledThreadPool(1) ;
		this.cs = CrakenScript.create(session, ses) ;
		cs.readDir(new File("./resource/ics6/script")) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		session.workspace().repository().shutdown() ;
		super.tearDown();
	}
}
