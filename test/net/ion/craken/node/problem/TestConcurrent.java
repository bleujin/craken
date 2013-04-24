package net.ion.craken.node.problem;

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
						WriteNode bleujin = wsession.root().addChild("/bleujin").property("name", "bleujin") ;
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
}
