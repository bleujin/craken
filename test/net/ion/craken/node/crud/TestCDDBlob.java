package net.ion.craken.node.crud;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import net.ion.craken.listener.AsyncCDDModifyHandler;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDModifyHandler;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.tree.impl.GridBlob;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.ObjectId;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.io.GridFilesystem;
import org.infinispan.notifications.cachelistener.event.Event.Type;
import org.jboss.netty.util.Timeout;

public class TestCDDBlob extends TestCase {

	public void testCallCDD() throws Exception {
		Craken craken = Craken.inmemoryCreateWithTest();
		ReadSession session = craken.login("test");

		final Workspace workspace = session.workspace();

		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/emp/{userid}";
			}

			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				try {
					Map<PropertyId, PropertyValue> values = event.getValue();
					for (PropertyId pid : values.keySet()) {
						PropertyValue val = values.get(pid);
						if (val.isBlob()) {
							InputStream input = ((GridBlob) val.workspace(workspace).asBlob()).toInputStream();
							Debug.line(pid, IOUtil.toStringWithClose(input));
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				return null;
			}
		});

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/bleujin").property("name", "bleujin").blob("stream", new FileInputStream("./resource/helloworld.txt"));
				return null;
			}

		});
	}

	public void testAtCraken() throws Exception {
		Craken craken = Craken.create() ;
		craken.createWorkspace("test", WorkspaceConfigBuilder.memoryDir().distMode(CacheMode.REPL_SYNC)) ;
		craken.createWorkspace("test2", WorkspaceConfigBuilder.memoryDir().distMode(CacheMode.REPL_SYNC)) ;
		craken.start() ;
		final ReadSession rsession1 = craken.login("test") ;
		final ReadSession rsession2 = craken.login("test2") ;
		final GridFilesystem gfs = rsession2.workspace().gfs() ;
		
		rsession1.workspace().cddm().add(new AsyncCDDModifyHandler("/request/{sid}/{uuid}") {
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, final CDDModifiedEvent event) {
				if (event.getKey().getType().isStructure()) return null; 
				final String sid = resolveMap.get("sid") ;
				final String uuid = resolveMap.get("uuid") ;
				final String script = event.property("script").asString() ;

				Debug.debug("received event", sid, uuid, event.etype());
				TransactionJob<Void> tjob = new TransactionJob<Void>(){
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						StringInputStream sinput = new StringInputStream(script.toUpperCase());
						wsession.pathBy("/response", sid, uuid).blob("result", sinput) ;
						IOUtil.close(sinput);
						Debug.debug("write response", sid, uuid, event.getKey());
						return null;
					}
				};
				rsession2.tran(tjob) ;
				return null ;
			}
		}) ;
		Debug.debug("started");
		
		new InfinityThread().startNJoin(); 
	}

	public void testAtScript() throws Exception {
		Craken craken = Craken.create();
		craken.start();
		craken.createWorkspace("test", WorkspaceConfigBuilder.memoryDir().distMode(CacheMode.REPL_SYNC));
		craken.createWorkspace("test2", WorkspaceConfigBuilder.memoryDir().distMode(CacheMode.REPL_SYNC));
		ReadSession rsession = craken.login("test");
		final ReadSession rsession2 = craken.login("test2");

		for (int i = 0; i < 10; i++) {

			final CountDownLatch cd = new CountDownLatch(1);
			final String uuid = new ObjectId().toString(); // "117";
			CDDModifyHandler listener = new CDDModifyHandler("/response/bleujin/" + uuid) {
				@Override
				public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
					if (event.getKey().getType().isData()) {
						Debug.debug("received", event.etype(), event.getKey());
						cd.countDown();
					}
					return null;
				}
			};
			rsession2.workspace().cddm().add(listener);
			long start = System.currentTimeMillis();
			rsession.tran(new TransactionJob<Void>() {
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/request/bleujin/" + uuid).property("script", "hello bleujin").property("age", 20);
					Debug.debug("started", new Date().getTime());
					return null;
				}
			});

			cd.await(5, TimeUnit.SECONDS);

			InputStream input = rsession2.pathBy("/response/bleujin/" + uuid).property("result").asBlob().toInputStream();
			Debug.line(IOUtil.toStringWithClose(input), System.currentTimeMillis() - start);
			rsession2.workspace().cddm().remove(listener);

		}

		Debug.debug("ended");

		// new InfinityThread().startNJoin();

	}

}
