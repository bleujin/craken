package net.ion.craken.tree;

import net.ion.craken.node.Repository;
import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.commands.VisitableCommand;
import org.infinispan.commands.tx.CommitCommand;
import org.infinispan.commands.tx.PrepareCommand;
import org.infinispan.config.ConfigurationException;
import org.infinispan.context.InvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.manager.DefaultCacheManager;

public class TestInterceptor {

	public static void createTreeCache(Repository repository, DefaultCacheManager dftManager, String cacheName) {

		// Validation to make sure that the cache is not null.
		Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache = dftManager.getCache(cacheName + ".node");
		if (cache == null) {
			throw new NullPointerException("The cache parameter passed in is null");
		}

		// If invocationBatching is not enabled, throw a new configuration exception.
		if (!cache.getCacheConfiguration().invocationBatching().enabled()) {
			throw new ConfigurationException("invocationBatching is not enabled for cache '" + cache.getName() + "'. Make sure this is enabled by" + " calling configurationBuilder.invocationBatching().enable()");
		}
		cache.getAdvancedCache().addInterceptor(new CustomCommandInvoker(), 0);

		// cache.addListener(repository.listener()) ;
		Cache<String, byte[]> blobdata = cache.getCacheManager().getCache(cacheName + ".blobdata");
		
	}
}

class CustomCommandInvoker extends BaseCustomInterceptor {

	protected Object handleDefault(InvocationContext ctx, VisitableCommand command) throws Throwable {
		switch(command.getCommandId()){
		
			case CommitCommand.COMMAND_ID :
				Debug.line("commit", command.getParameters()) ;
				break ;
			case PrepareCommand.COMMAND_ID :
				Debug.line("prepare" ,command.getParameters().length, command.getParameters(), ((PrepareCommand)command).getModifications()) ;
				break ;
			default :
				break ;
		}
		return invokeNextInterceptor(ctx, command);
	}
}