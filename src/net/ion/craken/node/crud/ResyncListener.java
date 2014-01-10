package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TranLogManager;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;
import org.infinispan.remoting.transport.Address;

@Listener
public class ResyncListener {

	private RepositoryImpl r;

	public ResyncListener(RepositoryImpl r) {
		this.r = r;
	}

	@ViewChanged
	public void viewChanged(ViewChangedEvent event) throws Exception {

		List<Address> removed = ListUtil.subtract(ObjectUtil.coalesce(event.getOldMembers(), ListUtil.newList()), event.getNewMembers());
		if (removed.size() > 0) {
			for (String wname : r.workspaceNames()) {
				ReadSession session = r.login(wname);
				session.workspace().tranLogManager().removeMember(removed);
			}
		}

		// for (String wname : r.workspaceNames()){
		// ReadSession session = r.login(wname);
		// session.workspace().tranLogManager().registerMember()
		//			
		//			
		// session.tranSync(new TransactionJob<Void>(){
		// @Override
		// public Void handle(WriteSession wsession) throws Exception {
		// wsession.iwconfig().inmemory(true) ;
		//					
		// Set<String> childNames = wsession.pathBy("/__servers").childrenNames();
		// for(String childName : childNames){
		// if (newAddressNames.contains(childName)) wsession.pathBy("/__servers/" + childName).property("repoid", r.repoId()) ;
		// else wsession.pathBy("/__servers/" + childName).removeSelf() ;
		// }
		// return null;
		// }
		// }) ;
		// }

	}

	public void inmomory() {
		r.executor().submitTask(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				for (String wname : r.workspaceNames()) {
					try {
						ReadSession session = r.login(wname);
						session.workspace().tranLogManager().registerMember("inmemory", r.repoId());
					} finally {
						r.release();
					}
				}
				return null;
			}
		});

	}

	@CacheStarted
	public void onStartedCache(final CacheStartedEvent event) throws IOException {
		if (!event.getCacheName().endsWith(".logmeta"))
			return;

		final String wname = StringUtil.substringBeforeLast(event.getCacheName(), ".");

		r.executor().submitTask(new Callable<Void>() {
			@Override
			public Void call() {
				try {

					final EmbeddedCacheManager cm = event.getCacheManager();
					final List<Address> members = cm.getMembers();

					ReadSession session = r.login(wname);
					// session.tranSync(new TransactionJob<Void>() {
					// @Override
					// public Void handle(WriteSession wsession) throws Exception {
					// wsession.iwconfig().inmemory(true) ;
					//							
					// wsession.pathBy("/__servers/" + r.addressId()).property("repoid", r.repoId()) ;
					// return null;
					// }
					// }) ;

					Workspace wspace = session.workspace();
					wspace.tranLogManager().registerMember(r.addressId(), r.repoId());

					if (members.size() <= 1)
						return null;

					int applied = wspace.tranLogManager().resync();
					r.putAttribute(TranLogManager.class.getSimpleName() + "." + wspace.wsName(), applied);

				} catch (Exception ex) {
					ex.printStackTrace();
					r.logger().warning("during to apply log, exception occured. \n" + ex.getMessage());
				} finally {
					r.release();
				}

				r.logger().info("applied log in workspace[" + wname + "]");
				return null;
			}
		});

	}
}
