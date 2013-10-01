package net.ion.craken.loaders.lucene.tran;

import java.util.Set;

import com.google.common.collect.Sets;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.ListUtil;

public class TestFindConfigInStore extends TestBaseCrud{

	
	public void testConfirm() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				return null;
			}
		}) ;
		
		
	}
	
	public void testLinkedHashSet() throws Exception {
		
		Set<String> sets = Sets.newLinkedHashSet() ;
		sets.addAll(ListUtil.toList("1", "2", "3", "3", "1")) ;
		
		Debug.line(sets); 
	}
}
