package net.ion.craken.node.search.util;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.ListUtil;

public class TransactionJobs {

	public final static TransactionJob<Void> dummy(final String prefixFqn, final int count){
		return new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				for (int i : ListUtil.rangeNum(count)) {
					wsession.root().addChild(prefixFqn + "/" + i).property("name", "bleujin").property("dummy", i) ;
				}
				return null;
			}
		} ;
	}
	public final static TransactionJob<Void> dummyBleujin(final int count){
		return dummy("/emp", count) ; 
	}
}
