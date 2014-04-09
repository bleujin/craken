package net.ion.craken.node.crud.util;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.ListUtil;

public class TransactionJobs {

	public static final TransactionJob<Void> HelloBleujin = new TransactionJob<Void>() {
		@Override
		public Void handle(WriteSession wsession) throws Exception {
			wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
			return null;
		}
	};

	public final static TransactionJob<Void> dummy(final String prefixFqn, final int count){
		return new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				for (int i : ListUtil.rangeNum(count)) {
					wsession.root().child(prefixFqn).property("prefix", prefixFqn).child("" + i).property("name", "bleujin").property("dummy", i) ;
				}
				return null;
			}
		} ;
	}
	
	public final static TransactionJob<Void> dummyEmp(final int count){
		return dummy("/emp", count) ; 
	}


}
