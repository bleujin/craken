package net.ion.craken.loaders;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;

/**
 * Single file cache store configuration builder.
 * 
 * @author Galder Zamarreño
 * @since 6.0
 */
public class CrakenStoreConfigurationBuilder extends AbstractStoreConfigurationBuilder<CrakenStoreConfiguration, CrakenStoreConfigurationBuilder> {

	private String location = "";
	private int maxEntries = -1;

	public CrakenStoreConfigurationBuilder(PersistenceConfigurationBuilder builder) {
		super(builder);
	}

	@Override
	public CrakenStoreConfigurationBuilder self() {
		return this;
	}

	/**
	 * Sets a location on disk where the store can write.
	 */
	public CrakenStoreConfigurationBuilder location(String location) {
		this.location = location;
		return this;
	}

	/**
	 * In order to speed up lookups, the single file cache store keeps an index of keys and their corresponding position in the file. To avoid this index resulting in memory consumption problems, this cache store can bounded by a maximum number of entries that it stores. If this limit is exceeded, entries are removed permanently using the LRU algorithm both from the in-memory index and the
	 * underlying file based cache store.
	 * 
	 * So, setting a maximum limit only makes sense when Infinispan is used as a cache, whose contents can be recomputed or they can be retrieved from the authoritative data store.
	 * 
	 * If this maximum limit is set when the Infinispan is used as an authoritative data store, it could lead to data loss, and hence it's not recommended for this use case.
	 */
	public CrakenStoreConfigurationBuilder maxEntries(int maxEntries) {
		this.maxEntries = maxEntries;
		return this;
	}

	@Override
	public CrakenStoreConfiguration create() {
		return new CrakenStoreConfiguration(purgeOnStartup, fetchPersistentState, ignoreModifications, async.create(), singletonStore.create(), preload, shared, properties, location, maxEntries);
	}

	@Override
	public Builder<?> read(CrakenStoreConfiguration template) {
		// CrakenStore-specific configuration
		location = template.location();
		maxEntries = template.maxEntries();
		// AbstractStore-specific configuration
		fetchPersistentState = template.fetchPersistentState();
		ignoreModifications = template.ignoreModifications();
		properties = template.properties();
		purgeOnStartup = template.purgeOnStartup();
		async.read(template.async());
		singletonStore.read(template.singletonStore());
		preload = template.preload();
		shared = template.shared();
		return this;
	}
}
