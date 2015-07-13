package net.ion.craken.node.crud;

import java.util.Date;
import java.util.Map;

import junit.framework.TestCase;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;

public class TestOtherWrite extends TestCase {

	public void testEvent() throws Exception {
		Craken craken = Craken.local();

		craken.createWorkspace("working", WorkspaceConfigBuilder.sifsDir("./resource/temp/sifs").maxEntry(100000));
		craken.createWorkspace("stat", WorkspaceConfigBuilder.gridDir("./resource/temp/grid"));

		ReadSession session = craken.login("working");
		final ReadSession stat = craken.login("stat");

		CDDHandler myhandler = new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/logs/{logid}";
			}
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				stat.tran(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession msession) throws Exception {
						Date date = new Date();
						msession.pathBy("/days", date.getMonth()+1).increase("count");
						msession.pathBy("/days", date.getMonth()+1, date.getDate()).increase("count");
						return null;
					}
				});
				return null;
			}

			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				return null;
			}
		};

		session.workspace().cddm().add(myhandler);

		for (int i = 0; i < 1; i++) {
			final int index = i;
			session.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/logs", index).property("name", "bleujin").property("time", System.currentTimeMillis());
					return null;
				}
			});
		}

//		stat.root().walkChildren().debugPrint();
		stat.root().childQuery("", true).find().debugPrint();

	}
}
