package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Workspace;
import net.ion.framework.util.StringUtil;

import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.remoting.transport.Address;

@Listener
public class ResyncListener {

	private RepositoryImpl r;

	public ResyncListener(RepositoryImpl r) {
		this.r = r;
	}

	@CacheStarted
	public void onStartedCache(final CacheStartedEvent event) throws IOException {
		if (!event.getCacheName().endsWith(".logmeta"))
			return ;

		final String wname = StringUtil.substringBeforeLast(event.getCacheName(), ".");
		
		r.executor().submitTask(new Callable<Void>() {
			@Override
			public Void call() {
				try {

					final EmbeddedCacheManager cm = event.getCacheManager();
					final List<Address> members = cm.getMembers();
					if (members.size() <= 1)
						return null;
					
					ReadSession session = r.login(wname);
					Workspace wspace = session.workspace();
					
					wspace.tranLogManager().resync() ;
					
				} catch (Exception ex) {
					ex.printStackTrace();
					r.logger().warning("during to apply log, exception occured. \n" + ex.getMessage()) ;
				} finally {
					r.release() ;
				}
				
				r.logger().info("applied log in workspae[" + wname + "]") ;
				return null;
			}
		});

	}
}
