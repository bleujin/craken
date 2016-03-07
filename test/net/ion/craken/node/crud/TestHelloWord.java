package net.ion.craken.node.crud;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey.Type;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.db.Rows;
import net.ion.framework.util.IOUtil;

import org.infinispan.atomic.impl.AtomicHashMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

public class TestHelloWord extends TestCase {

	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.inmemoryCreateWithTest() ;
		r.start() ;
		this.session = r.login("test") ;
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testHello() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/hello").property("greeting", "Hello World") ;
				return null;
			}
		}) ;
		
		assertEquals("Hello World", session.pathBy("/hello").property("greeting").value()) ;
	}
	
	public void testResultSet() throws Exception {
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.pathBy("/emps/hero").property("name", "hero").property("age", 21)
					.child("address").property("city", "seoul") ;
				return null;
			}
		}) ;
		
		ResultSet rs = session.pathBy("/emps")
			.children().gte("age", 10).toAdRows("name, age, address.city cname") ;
		((Rows)rs).debugPrint() ;
	}
	
	
	public void testIO() throws Exception {
		final InputStream src = new StringInputStream("LongLongString");
		
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws UnsupportedEncodingException {
				wsession.pathBy("/greet").property("greeting", "Hello World")
					.blob("file", src);
				return null;
			}
		}) ;
		
		final InputStream readed = session.pathBy("/greet").property("file")
			.asBlob().toInputStream();
		assertEquals("LongLongString", IOUtil.toStringWithClose(readed)) ;
	}
	
	
	public void testQuery() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws UnsupportedEncodingException {
				wsession.pathBy("/query/1").property("greeting", "태극기가 바람에 펄럭입니다.")
					.property("since", 20);
				wsession.pathBy("/query/2").property("greeting", "태극기가 바람에 펄럭입니다.") ;
				return null;
			}
		}) ;
		
		session.pathBy("/query").childQuery("greeting:태극기").between("since", 10, 30)
			.ascending("since").find().debugPrint() ;
	}
	
	public void testListener() throws Exception {
		final DebugListener listener = new DebugListener();
		session.workspace().addListener(listener) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("bleujin").property("name", "bleujin");
				return null ;
			}
		}).get() ;
		
		assertEquals(1, listener.getCount()) ;
	}
	
	@Listener
	static public class DebugListener implements WorkspaceListener{
		private AtomicInteger counter = new AtomicInteger() ;
		@CacheEntryModified
		public void modified(CacheEntryModifiedEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> e){
			if (e.isPre()) return ;
			if (e.getKey().getType() == Type.DATA)  {
				counter.incrementAndGet() ;
			}
		}
		
		public int getCount(){
			return counter.get() ;
		}

		@Override
		public void registered(Workspace workspace) {
			
			
		}

		@Override
		public void unRegistered(Workspace workspace) {
			
			
		}
	}
}
