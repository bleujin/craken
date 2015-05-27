package net.ion.craken.loaders;

import java.util.Properties;

import net.ion.nsearcher.config.Central;

import org.infinispan.commons.configuration.BuiltBy;
import org.infinispan.commons.configuration.ConfigurationFor;
import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.SingletonStoreConfiguration;

/**
 * Defines the configuration for the single file cache store.
 * @since 6.0
 */
@BuiltBy(CrakenStoreConfigurationBuilder.class)
@ConfigurationFor(CrakenStore.class)
@Deprecated
public class CrakenStoreConfiguration extends AStoreConfiguration {
	private final String location;
	private final int maxEntries;
	private CrakenStore store;

	public CrakenStoreConfiguration(boolean purgeOnStartup, boolean fetchPersistentState, boolean ignoreModifications, AsyncStoreConfiguration async, SingletonStoreConfiguration singletonStore, boolean preload, boolean shared, Properties properties, String location, int maxEntries) {
		super(purgeOnStartup, fetchPersistentState, ignoreModifications, async, singletonStore, preload, shared, properties);
		this.location = location;
		this.maxEntries = maxEntries;
		
	}

	public String location() {
		return location;
	}

	public int maxEntries() {
		return maxEntries;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;
		CrakenStoreConfiguration that = (CrakenStoreConfiguration) o;
		if (maxEntries != that.maxEntries)
			return false;
		if (location != null ? !location.equals(that.location) : that.location != null)
			return false;
		return true;
		
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (location != null ? location.hashCode() : 0);
		result = 31 * result + maxEntries;
		return result;
	}

	@Override
	public String toString() {
		return "CrakenStoreConfiguration{" + "location='" + location + '\'' + ", maxEntries=" + maxEntries + '}';
	}
	
	public void store(CrakenStore searchStore) {
		this.store = searchStore ;
	}
	
	public Central central(){
		return store.central() ;
	}
}
