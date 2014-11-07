package net.ion.craken.loaders;

import java.util.Properties;

import net.ion.nsearcher.config.Central;

import org.infinispan.configuration.cache.AbstractStoreConfiguration;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.SingletonStoreConfiguration;

public abstract class AStoreConfiguration extends AbstractStoreConfiguration {

	public AStoreConfiguration(boolean purgeOnStartup, boolean fetchPersistentState, boolean ignoreModifications, AsyncStoreConfiguration async, SingletonStoreConfiguration singletonStore, boolean preload, boolean shared, Properties properties) {
		super(purgeOnStartup, fetchPersistentState, ignoreModifications, async, singletonStore, preload, shared, properties);
	}

	public abstract Central central()  ;
	
	

}
