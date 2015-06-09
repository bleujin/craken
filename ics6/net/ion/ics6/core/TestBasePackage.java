package net.ion.ics6.core;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.script.CrakenScript;

public class TestBasePackage extends TestCase{

	
	private Craken r;
	private ScheduledExecutorService ses;

	protected ReadSession session;
	protected CrakenScript cs;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.inmemoryCreateWithTest() ;
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
