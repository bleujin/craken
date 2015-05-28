package net.ion.craken.loaders;

import org.infinispan.commons.configuration.Builder;
import org.infinispan.configuration.cache.AbstractStoreConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;

@Deprecated
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
