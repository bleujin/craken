package net.ion.craken.aradon;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.nradon.WebSocketConnection;
import net.ion.radon.core.Aradon;
import net.ion.radon.util.AradonTester;

import org.testng.Assert;

public class TestTalkEngine extends TestCase {

	private TalkEngine engine;
	WebSocketConnection bleujin = FakeConnection.create("bleujin");

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.engine = TalkEngine.test();
		engine.registerHandler(new DummyHandler()) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		engine.stop() ;
		super.tearDown();
	}
	
	public void testUse() throws Exception {
		engine.onOpen(bleujin) ;
		engine.onMessage(bleujin, "hello") ;
		engine.onClose(bleujin) ;
	}
	
	
	public void testConnectionManger() throws Exception {
		engine.onOpen(bleujin) ;

		assertTrue(engine.connManger().findBy(bleujin) != null) ;
		assertTrue(engine.connManger().findBy("bleujin") != null) ;
		engine.onClose(bleujin) ;
		assertTrue(engine.connManger().findBy(bleujin) == null) ;
		assertTrue(engine.connManger().findBy("bleujin") == null) ;
	}

	
}

class DummyHandler implements TalkHandler {

	@Override
	public void onClose(TalkEngine tengine, UserConnection uconn) {
		Assert.assertEquals("bleujin", uconn.id()) ;
	}

	@Override
	public void onMessage(TalkEngine tengine, UserConnection uconn, ReadSession rsession, TalkMessage tmessage) {
		Assert.assertEquals("bleujin", uconn.id()) ;
	}

	@Override
	public void onConnected(TalkEngine tengine, UserConnection uconn) {
		Assert.assertEquals("bleujin", uconn.id()) ;
	}

	@Override
	public void onEngineStart(TalkEngine tengine) {
			
	}

	@Override
	public void onEngineStop(TalkEngine tengine) {
		
	}
}
