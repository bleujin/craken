package net.ion.craken.node.problem;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectId;

public class TestConcurrent extends TestBaseCrud {

	
	public void testRoot() throws Exception {
		Runnable task = new Runnable(){
			@Override
			public void run() {
				session.tran(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) {
						WriteNode root = wsession.root();
						Debug.line(root.id(), "root") ;
						return null;
					}
				}) ;
			}
		} ;
		
		ExecutorService es = Executors.newFixedThreadPool(3) ;
		for (int i : ListUtil.rangeNum(100)) {
			es.submit(task) ;
		}
		
		es.awaitTermination(3, TimeUnit.SECONDS) ;
	}
	
	
	public void testId() throws Exception {
		Debug.line(session.root().id()) ;
	}
	
	public void testGetNode() throws Exception {
		Runnable wtask = new Runnable(){
			@Override
			public void run() {
				session.tran(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) {
						WriteNode bleujin = wsession.root().child("/bleujin").property("name", "bleujin") ;
						Debug.debug(bleujin.id(), bleujin.parent().id(), wsession.root().id()) ;
						return null;
					}
				}) ;
			}
		} ;

		ExecutorService wes = Executors.newFixedThreadPool(5) ;
		for (int i : ListUtil.rangeNum(100)) {
			wes.submit(wtask) ;
		}

		wes.awaitTermination(2, TimeUnit.SECONDS) ;
	}
	
	public void testConcurrent() throws Exception {
		Runnable wtask = new Runnable(){
			@Override
			public void run() {
				session.tran(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) {
						WriteNode bleujin = wsession.pathBy("/bleujin").child(new ObjectId().toString()).property("name", "bleujin") ;
						return null;
					}
				}) ;
			}
		} ;
		
		Runnable rtask = new Runnable(){
			@Override
			public void run() {
				Debug.line(session.ghostBy("/bleujin").children().toList().size());
			}
		} ;
		
		ExecutorService wes = Executors.newFixedThreadPool(5) ;
		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				Debug.line(t, e) ;
			}
		}) ;
		for (int i : ListUtil.rangeNum(100)) {
			wes.submit(wtask) ;
			wes.submit(rtask) ;
		}
		
		wes.awaitTermination(2, TimeUnit.SECONDS) ;
	}
	
	
	public void testConcurrentReadWrite() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("age", 20) ;
				return null;
			}
		}) ;
		ReadNode bleujin = session.pathBy("/bleujin");
		Debug.line(bleujin.property("age")) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("age", 30) ;
				return null;
			}
		}) ;
		Debug.line(bleujin.property("age")) ;
	}
	
}
